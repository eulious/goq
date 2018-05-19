import java.util.Deque
import java.util.LinkedList
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import javafx.scene.text.Font

/**
 * 盤面を生成するクラス. Singleton.
 * 全てのメソッドで, パスの手が渡されることは想定されていない.
 *
 * @author eulious
 */
object Board {
    private lateinit var canvas: Canvas
    private lateinit var gc: GraphicsContext
    var boardsize = 19
        set(boardsize) {
            field = boardsize
            this.board = Array(boardsize) { IntArray(boardsize) }
        }
    private var board = Array(boardsize) { IntArray(boardsize) }
    private var radius: Double = 0.toDouble()

    fun initCanvas(canvas: Canvas) {
        this.canvas = canvas
        gc = canvas.graphicsContext2D
    }

    /**
     * 盤面の２次元配列を生成し、盤面に出力.
     *
     * 他のクラスから盤面に石を置くにはこのメソッドを使う.
     * board[][] 黒: 1, 白: 0, 空点: 2 の配列.
     * @see "Record.show
     * @param kihumove 指し手の動的配列.
     * @param kihublack 黒:true, 白:falseの動的配列.
     */
    fun makeboard(kihumove: Deque<String>, kihublack: Deque<Boolean>) {
        for (i in 0 until boardsize) {
            for (j in 0 until boardsize) {
                board[i][j] = 0
            }
        }
        val removelist = LinkedList<Int>()
        val its = kihumove.iterator()
        val isblack = kihublack.iterator()
        var color = 1

        fun check(x: Int, y: Int, color: Int): Boolean {
            if (x < 0 || x > board.size - 1 || y < 0 || y > board.size - 1) { // 外 -> true
                return true
            }
            if (board[y][x] == color) { // 黒 -> false
                return true
            }
            if (board[y][x] == -color) { // 白 -> 四方を検索(再帰)
                removelist.add(x * board.size + y)
                val tmp = booleanArrayOf(true, true, true, true) // 調べた -> true
                if (removelist.indexOf((x - 1) * board.size + y) == -1) {
                    tmp[0] = check(x - 1, y, color)
                }
                if (removelist.indexOf((x + 1) * board.size + y) == -1) {
                    tmp[1] = check(x + 1, y, color)
                }
                if (removelist.indexOf(x * board.size + (y - 1)) == -1) {
                    tmp[2] = check(x, y - 1, color)
                }
                if (removelist.indexOf(x * board.size + (y + 1)) == -1) {
                    tmp[3] = check(x, y + 1, color)
                }
                return tmp[0] and tmp[1] and tmp[2] and tmp[3]
            }
            return false // 空 -> false
        }

        while (its.hasNext()) {
            val pv = its.next() as String
            val x = pv[0].toInt() - 65
            val y = boardsize - Integer.parseInt(pv.substring(1))
            color = if (isblack.next()) 1 else -1

            board[y][x] = color
            val tmps = arrayOf(intArrayOf(x - 1, y), intArrayOf(x + 1, y), intArrayOf(x, y - 1), intArrayOf(x, y + 1))
            for (tmp in tmps) {
                removelist.clear()
                if (check(tmp[0], tmp[1], color)) {
                    removelist.forEach { tmp1 -> board[tmp1 % 9][tmp1 / 9] = 0 }
                }
            }
        }
        radius = canvas.height / (this.boardsize * 2)

        gc.fill = Color.WHITE
        gc.fillRect(0.0, 0.0, canvas.height, canvas.width)
        gc.stroke = Color.BLACK
        gc.lineWidth = 1.0
        for (i in 0 until this.boardsize) {  // 横線
            gc.strokeLine(radius, radius * (2 * i + 1),
                    radius * (2 * this.boardsize - 1), radius * (2 * i + 1))
        }
        for (i in 0 until this.boardsize) { // 縦線
            gc.strokeLine(radius * (2 * i + 1), radius,
                    radius * (2 * i + 1), radius * (2 * this.boardsize - 1))
        }

        for (x in 0 until this.boardsize) {
            for (y in 0 until this.boardsize) {
                if (board[y][x] == 1) { // 黒
                    gc.fill = Color.BLACK
                    gc.stroke = Color.BLACK
                    gc.fillOval(2.0 * radius * x.toDouble(), 2.0 * radius * y.toDouble(), 2 * radius, 2 * radius)
                    gc.strokeOval(2.0 * radius * x.toDouble(), 2.0 * radius * y.toDouble(), 2 * radius, 2 * radius)
                } else if (board[y][x] == -1) { // 白
                    gc.fill = Color.WHITE
                    gc.stroke = Color.BLACK
                    gc.fillOval(2.0 * radius * x.toDouble(), 2.0 * radius * y.toDouble(), 2 * radius, 2 * radius)
                    gc.strokeOval(2.0 * radius * x.toDouble(), 2.0 * radius * y.toDouble(), 2 * radius, 2 * radius)
                }
            }
        }
    }

    /**
     * 予想手, 指し手を盤面にアルファベットabcで表示する. 予想手がa, 実際の指し手がb, 同じならcを盤面に表示する.
     *
     * @param a 予想手
     * @param b 実際の指し手
     * @see "Record.show
     */
    fun abc(a: String, b: String) {
        gc.font = Font.font("Verdana", radius * 2.5)
        gc.fill = Color.BLACK
        val ax = a[0].toInt() - 65
        val bx = b[0].toInt() - 65
        val ay = boardsize - Integer.parseInt(a.substring(1)) + 1
        val by = boardsize - Integer.parseInt(b.substring(1)) + 1

        if (a == b) {
            gc.fillText("c", radius * (2 * ax + 0.25), radius * (2 * ay - 0.25))
        } else {
            gc.fillText("a", radius * (2 * ax + 0.25), radius * (2 * ay - 0.25))
            gc.fillText("b", radius * (2 * bx + 0.25), radius * (2 * by - 0.25))
        }
    }

    /**
     * クリックされた座標から符号を返す.
     * @param x x座標
     * @param y y座標
     * @return 符号, 例:"E13"
     */
    fun getClickedsign(x: Double, y: Double): String {
        val posx = Math.round((x - radius) / (2 * radius)).toInt()
        val posy = Math.round((y - radius) / (2 * radius)).toInt()
        return (posx + 65).toChar() + (this.boardsize - posy).toString()
    }
}