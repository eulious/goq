import java.util.Comparator
import java.util.HashMap
import java.util.TreeMap

import javafx.beans.property.IntegerProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.Label
import javafx.scene.control.TableView
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.paint.Color

object View {
    private lateinit var treeView: TreeView<*>
    private lateinit var genlabel: Label
    private lateinit var graph: Canvas
    private lateinit var gc: GraphicsContext
    private lateinit var leafView: TableView<Leaf>

    private val tm = TreeMap<String, Tree>()
    private val lecont = FXCollections.observableArrayList<Leaf>()

    fun initContents(graph: Canvas, leafView: TableView<*>,
                     treeView: TreeView<String>, genlabel: Label){
        this.graph = graph
        this.leafView = leafView as TableView<Leaf>
        this.leafView.items = lecont
        this.treeView = treeView
        this.genlabel = genlabel
        this.gc = graph.graphicsContext2D
    }

    fun getClickedbranch(ti: Any): String {
        val tmp = ti as Tree
        return tmp.id
    }

    fun addTree(key: String, br: Branch) {
        val color = if (br.itisBlacks().next()) "●" else "○"
        val move = br.itMoves().next()
        val tr = Tree(br.hide.toString() + color + move, key)
        tm[key] = tr
        val tl: ObservableList<TreeItem<String>>
        if (key.length == 3) {
            tl = tm["0"]!!.children
        } else {
            tl = tm[key.substring(0, key.length - 3)]!!.children
        }
        tl.add(tr)
        tl.sortWith(Comparator.comparing<TreeItem<String>, String> { t -> t.value })
    }

    fun makeLeaf(leaf: Int, br: Branch) {
        lecont.clear()
        val itb = br.itisBlacks()
        val its = br.itMoves()
        val nowhand = leaf
        var i = 0
        while (its.hasNext()) {
            lecont.add(Leaf(br.hide + i, itb.next(), its.next()))
            i++
        }
        leafView.selectionModel.select(leaf)
    }

    /**
     * 予想手のラベルに評価値, 指し手を描画する.
     * Record.show()から呼ばれる.
     */
    fun genmove(br: Branch) {
        var str = br.eval.toString() + ":"
        val its = br.itMoves()
        val itb = br.itisBlacks()
        while (its.hasNext()) {
            str += if (itb.next()) " ●" else " ○"
            str += its.next()
        }
        str = if (str.length > 36) str.substring(0, 36) + " ..." else str
        genlabel.text = str
    }

    /**
     * 評価値のグラフを描画する
     * Record.show()から呼ばれる.
     * @param hand 現在の手数
     * @param genmap 予想手の連想配列
     */
    fun graph(hand: Int, genmap: HashMap<Int, Branch>) {
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
                    pos, graph.height / 2 * (1 - value.eval as Double / 1000.0)
            )
            pos += interval
        }
        gc.lineWidth = 1.0
        gc.strokeLine(interval * hand, 0.0, interval * hand, graph.height)
    }

    internal fun setLeaf(leaf: Int) {
        throw UnsupportedOperationException("Not supported yet.")
    }

    internal fun removeTree(branch: String) {
        throw UnsupportedOperationException("Not supported yet.")
    }

    private class Tree(text: String, val id: String) : TreeItem<String>(text)

    private class Leaf(hand: Int, isblack: Boolean, move: String) {
        private val handdisp: IntegerProperty
        private val colordisp: StringProperty
        private val pvdisp: StringProperty
        fun handdispProperty(): IntegerProperty = handdisp
        fun colordispProperty(): StringProperty = colordisp
        fun pvdispProperty(): StringProperty = pvdisp
        init {
            this.handdisp = SimpleIntegerProperty(hand)
            this.colordisp = SimpleStringProperty(if (isblack) "●" else "○")
            this.pvdisp = SimpleStringProperty(move)
        }
    }
}