import javafx.beans.property.IntegerProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.collections.FXCollections
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.Label
import javafx.scene.control.TableView
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.paint.Color


/**
 * 予想手のラベルに評価値, 指し手を描画する.
 * Record.show()から呼ばれる.
 */
object Genmove {
    private lateinit var genlabel: Label
    private lateinit var graph: Canvas
    private lateinit var gc: GraphicsContext

    var genmap = HashMap<Int, Branch>()
    var isGenmove = false
        set(isGenmove){
            field = isGenmove && genmap.isNotEmpty()
        }

    fun initGenmove(graph: Canvas, genlabel: Label) {
        this.genlabel = genlabel
        genlabel.setOnMouseClicked {
            Record.setPonderMoves(genmap[Record.br.hand]!!)
        }
        this.graph = graph
        gc = graph.graphicsContext2D
    }

    fun genmove(hash: Int) {
        if (!isGenmove) return
        val br = genmap[hash]!!
        var str = br.eval.toString() + ":"
        val its = br.moves.iterator()
        val itb = br.isblacks.iterator()
        while (its.hasNext()) {
            str += if (itb.next()) " ●" else " ○"
            str += its.next()
        }
        str = if (str.length > 36) str.substring(0, 36) + " ..." else str
        genlabel.text = str
    }

    fun getPonderMove(hand : Int) = genmap[hand]!!.move

    /**
     * 評価値のグラフを描画する
     * Record.show()から呼ばれる.
     * @param hand 現在の手数
     * @param genmap 予想手の連想配列
     */
    fun graph(hand: Int) {
        if (!isGenmove) return
        val it = genmap.keys.iterator()
        val interval = 6.0
        gc.fill = Color.WHITE
        gc.fillRect(0.0, 0.0, graph.width, graph.height)
        gc.stroke = Color.BLACK
        gc.lineWidth = 4.0
        while (it.hasNext()) {
            val key = it.next()
            val value = genmap[key]!!
            var pos = interval * key
            gc.strokeLine(
                    pos, graph.height / 2,
                    pos, graph.height / 2 * (1 - value.eval / 1000.0)
            )
            pos += interval
        }
        gc.lineWidth = 1.0
        gc.strokeLine(interval * hand, 0.0, interval * hand, graph.height)
    }
}


object Table {
    private lateinit var table: TableView<Leaf>
    private val lecont = FXCollections.observableArrayList<Leaf>()

    fun initTable(table: TableView<Leaf>){
        this.table = table
        this.table.items = lecont
    }

    fun update(br: Branch) {
        lecont.clear()
        val itb = br.itisBlacks()
        val its = br.itMoves()
        var i = 0
        while (its.hasNext()) {
            lecont.add(Leaf(br.hide + i++, itb.next(), its.next()))
        }
        table.selectionModel.select(br.hand - br.hide -1)
    }
}

class Leaf(hand: Int, isblack: Boolean, move: String) {
    private val handdisp: IntegerProperty
    private val colordisp: StringProperty
    private val pvdisp: StringProperty
    fun handdispProperty(): IntegerProperty = handdisp
    fun colordispProperty(): StringProperty = colordisp
    fun pvdispProperty(): StringProperty = pvdisp
    init {
        this.handdisp = SimpleIntegerProperty(hand + 1)
        this.colordisp = SimpleStringProperty(if (isblack) "●" else "○")
        this.pvdisp = SimpleStringProperty(move)
    }
}

object Tree {
    private lateinit var tree: TreeView<String>
    private lateinit var selected: TreeItem<String>

    fun initTree(tree: TreeView<String>){
        this.tree = tree
        tree.root = TreeItem("本譜　　　　　　　　　　　　0")
        tree.root.isExpanded = true
        tree.selectionModel.select(0)
        tree.selectionModel.selectedItemProperty().addListener { _ ->
            selected = tree.selectionModel.selectedItem
            Record.hash = selected.value.split("　").last().toInt()
        }
    }

    fun addTree(br: Branch, hash: Int){
        val hand = br.hide + 1
        val eval = br.eval
        val isblack = if (br.isblack) "●" else "○"
        val move = br.move
        selected = tree.selectionModel.selectedItem
        selected.children.add(TreeItem(
                "$hand,　$eval,　$isblack$move　　　　　　　　　　$hash"
        ))
        selected.isExpanded = true
        tree.selectionModel.select(hash)
        tree.root.children.sortWith(Comparator.comparing<TreeItem<String>, String> { t -> t.value })
    }

    fun delete(){ selected.parent.children.remove(selected) }
}