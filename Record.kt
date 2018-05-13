import java.util.Deque
import java.util.ArrayDeque

object Record {
    private val brdata = ArrayList<Branch>()
    lateinit var br : Branch

    var hash = 0
        set(hash){
            field = hash
            br = brdata[hash]
            Genmove.isGenmove = (hash == 0)
            makeboard()
        }

    /**
     * 指し手の移動. マウスホイールが回されたときに呼ばれる
     * @param isNext 下方向ならtrue 上方向ならfalse
     */
    fun next(isNext: Boolean) {
        if (isNext && br.hand < br.moves.size - 1) {
            br.hand += 1
        } else if (!isNext && br.hide < br.hand) {
            br.hand -= 1
        } else {
            return
        }
        Table.update(br)
        makeboard()
    }

    /**
     * 盤面がクリックされたときに呼ばれる.
     * @param sign クリックした符号
     */
    fun add(sign: String) {
        if (br.hand != br.moves.size - 1) { // 新しい分岐を追加
            val newmoves = ArrayDeque<String>()
            val newisblacks = ArrayDeque<Boolean>()
            val its = br.moves.iterator()
            val itb = br.isblacks.iterator()
            for (i in 0 until br.hand + 1) {
                newmoves.add(its.next())
                newisblacks.add(itb.next())
            }
            newisblacks.add(!newisblacks.last)
            newmoves.add(sign)
            newBranch(Branch(br.hand + 1, 0, newisblacks, newmoves))
        } else {
            br.moves.add(sign)
            br.isblacks.add(!br.isblacks.last)
            br.hand += 1
        }
        Table.update(br)
    }

    /**
     * 現在の状態を画面に描画する.
     * 盤面の更新, 予想手と実際の指し手の表示, 指し手リストの表示, 予想手の表示
     */
    private fun makeboard() {
        val kihumove = ArrayDeque<String>()
        val kihublack = ArrayDeque<Boolean>()
        val its = br.moves.iterator()
        val itb = br.isblacks.iterator()
        for (i in 0 until br.hand) {
            kihumove.add(its.next())
            kihublack.add(itb.next())
        }
        Board.makeboard(kihumove, kihublack)
        if (Genmove.isGenmove) {
            // TODO : 最終手でArrayIndexOutOfBoundsException
            Board.abc(its.next(), Genmove.getPonderMove(br.hand))
            Genmove.genmove(br.hand + 1)
            Genmove.graph(br.hand)
        }
    }

    fun adoptSearch(eval: Int, isblacks: Deque<Boolean>, moves: Deque<String>) {
        val newmoves = ArrayDeque<String>()
        val newisblacks = ArrayDeque<Boolean>()
        val its = br.moves.iterator()
        val itb = br.isblacks.iterator()
        for (i in 0 until br.hand + 1) {
            newmoves.add(its.next())
            newisblacks.add(itb.next())
        }
        newisblacks.addAll(isblacks)
        newmoves.addAll(moves)
        newBranch(Branch(br.hand, eval, newisblacks, newmoves))
    }

    private fun newBranch(newbr: Branch){
        brdata.add(newbr)
        hash = brdata.lastIndex
        Tree.addTree(newbr, brdata.lastIndex)
        Table.update(newbr)
    }

    fun setHonpu(honpu: Deque<String>, honpublack: Deque<Boolean>) {
        brdata.add(Branch(0, 0, honpublack, honpu))
        hash = brdata.lastIndex
        Table.update(Branch(0, 0, honpublack, honpu))
    }

    fun getNowBranch(): Pair<Deque<String>, Deque<Boolean>> {
        return Pair(br.moves, br.isblacks)
    }
}

class Branch(val hide: Int, val eval: Int, val isblacks: Deque<Boolean>, val moves: Deque<String>) {
    val isblack = itisBlacks().next()
    val move = itMoves().next()
    var hand = hide
    fun itisBlacks(): Iterator<Boolean> {
        val it = isblacks.iterator()
        for (i in 0 until hide) it.next()
        return it
    }
    fun itMoves(): Iterator<String> {
        val it = moves.iterator()
        for (i in 0 until hide) it.next()
        return it
    }
}