import java.util.Deque
import java.util.ArrayDeque

/**
 * TODO : TreeViewの実装
 */
object Record {
    private var hand = 0
    private val brdata = HashMap<String, Branch>()
    var genmap = HashMap<Int, Branch>()

    private var branch = "0"
        set(branch){
            field = branch
            hand = if (isHonpu()) leaf else hand
            leaf = if (isHonpu()) hand else brdata[branch]!!.hide
        }

    private var leaf = 0
        set(leaf) {
            field = leaf
            View.setLeaf(leaf)
            makeboard()
        }

    /**
     * 指し手の移動. マウスホイールが回されたときに呼ばれる
     * @param isnext 下方向ならtrue 上方向ならfalse
     */
    fun next(isnext: Boolean) {
        val br = brdata[branch]!!
        if (isnext && leaf < br.moves.size - 1) {
            leaf += 1
        } else if (!isnext && br.hide < leaf) {
            leaf -= 1
        } else {
            return
        }
        View.setLeaf(leaf)
    }

    /**
     * 盤面がクリックされたときに呼ばれる.
     * @param sign クリックした符号
     */
    fun add(sign: String) {
        val br = brdata[branch]!!
        if (leaf != br.moves.size - 1) { // 新しい分岐を追加
            val newmoves = ArrayDeque<String>()
            val newisblacks = ArrayDeque<Boolean>()
            val its = br.moves.iterator()
            val itb = br.isblacks.iterator()
            for (i in 0 until leaf + 1) {
                newmoves.add(its.next())
                newisblacks.add(itb.next())
            }
            newisblacks.add(!newisblacks.last)
            newmoves.add(sign)
            addMap(Branch(leaf + 1, 0, newisblacks, newmoves))
        } else {
            br.moves.add(sign)
            br.isblacks.add(!br.isblacks.last)
            leaf += 1
            View.setLeaf(leaf)
        }
    }

    /**
     * 現在の状態を画面に描画する.
     * 盤面の更新, 予想手と実際の指し手の表示, 指し手リストの表示, 予想手の表示
     */
    private fun makeboard() {
        val kihumove = ArrayDeque<String>()
        val kihublack = ArrayDeque<Boolean>()
        val its = brdata[branch]!!.moves.iterator()
        val itb = brdata[branch]!!.isblacks.iterator()
        for (i in 0 until leaf) {
            kihumove.add(its.next())
            kihublack.add(itb.next())
        }
        Board.makeboard(kihumove, kihublack)
        if (isHonpu() && !genmap.isEmpty()) {
            if (genmap.containsKey(hand + 1)) {
                // TODO : 最終手でArrayIndexOutOfBoundsException
                Board.abc(its.next(), genmap[hand + 1]!!.moves.iterator().next())
                View.genmove(genmap[hand + 1]!!)
            }
            View.graph(hand, genmap)
        }
    }

    /**
     * 分岐を削除する.
     * 構造体のdeleteを真にして, 表を作るときにそれを読み込まないようにする.
     */
    fun deleteBranch(branch: String) {
        if (branch != "0") {
            brdata.remove(branch)
            this.branch = "0"
            View.removeTree(branch)
        }
    }

    fun adoptGenmove() {
        addMap(Branch(hand,
                genmap[hand + 1]!!.eval,
                genmap[hand + 1]!!.isblacks,
                genmap[hand + 1]!!.moves
        ))
    }

    fun adoptSearch(eval: Int, isblacks: Deque<Boolean>, moves: Deque<String>) {
        val newmoves = ArrayDeque<String>()
        val newisblacks = ArrayDeque<Boolean>()
        val its = brdata[branch]!!.moves.iterator()
        val itb = brdata[branch]!!.isblacks.iterator()
        for (i in 0 until leaf + 1) {
            newmoves.add(its.next())
            newisblacks.add(itb.next())
        }
        newisblacks.addAll(isblacks)
        newmoves.addAll(moves)
        addMap(Branch(hand, eval, newisblacks, newmoves))
    }

    private fun addMap(br: Branch) {
        var keyint = 1
        var newkey = branch + String.format("%03d", keyint)
        while (!brdata.containsKey(newkey)) {
            newkey = branch + String.format("%03d", keyint++)
        }
        brdata[newkey] = br
        branch = newkey
    }

    private fun isHonpu() : Boolean = branch == "0"

    fun setHonpu(honpu: Deque<String>, honpublack: Deque<Boolean>) {
        println("Record.setHonpu")
        brdata["0"] = Branch(hand, 0, honpublack, honpu)
        makeboard()
    }

    fun getNowBranch() : Pair<Deque<String>, Deque<Boolean>> {
        return Pair(brdata[branch]!!.moves, brdata[branch]!!.isblacks)
    }
}

class Branch(val hide: Int, val eval: Int, val isblacks: Deque<Boolean>, val moves: Deque<String>) {
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