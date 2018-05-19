import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.ArrayDeque
import java.util.Deque


/**
 * GTPプロトコルでやり取りを行うクラス. Singleton.
 * 注意：Branch は枝ではなく, AIから返ってきた指し手・評価値を格納するものとして使用している.
 *
 * @author eulious
 */
object Ponder {
    private var isGote = false
    private var boardsize = 19
        set(boardsize){
            Board.boardsize = boardsize
            field = boardsize
        }

    fun paste() {
        val sgf = readFile("sample.sgf").replace("\n", "").replace(")","").split(";")
        boardsize = sgf[1]
                .replace(".*SZ\\[".toRegex(), "")
                .replace("].*".toRegex(), "")
                .toInt()
        isGote = Regex("PW\\[euli").matches(sgf[1])
        fun a9b8(s:Char) : String = (boardsize - s.toInt() + 97).toString()
        fun revearse(s:Char) : Char = (boardsize - s.toInt() + 193).toChar()
        val moves : Deque<String> = ArrayDeque<String>()
        val isblacks : Deque<Boolean> = ArrayDeque<Boolean>()
        for (hand in sgf.slice(2 until sgf.size)){
            if (hand[2] == ']') continue
            isblacks.add(hand[0] == 'B')
            moves.add(hand.let {
                if (isGote) {
                    revearse(it[1]).toUpperCase().toString() + a9b8(revearse(it[2]))
                } else {
                    it[2].toUpperCase().toString() + a9b8(it[3])
                }
            })
        }
        Record.setHonpu(moves, isblacks)
    }

    private fun gtp(sgf:String, lefttime:Double, isblack:Boolean): Pair<Double, Branch> {
        val DIR = "."

        val correctALP = mapOf("A" to "A", "B" to "B", "C" to "C", "D" to "D",
                "E" to "E", "F" to "F", "G" to "G", "H" to "H", "J" to "I",
                "K" to "J", "L" to "K", "M" to "L", "N" to "M", "O" to "N",
                "P" to "O", "Q" to "P", "R" to "Q", "S" to "R", "T" to "S"
        )

        writeFile("test.sgf", "$sgf)")
        writeFile("test.exp", """
        #!/usr/bin/expect
        set timeout 20
        spawn env LANG=C $DIR/leela_0110_macOS_opencl -g
        send "known_command\n"
        expect true
        send "loadsgf $DIR/test.sgf\n"
        send "time_settings ${lefttime.toInt()} 0 0\n"
        send "showboard\n"
        expect Hash
        send "genmove ${if (isblack) "black" else "white"}\n"
        expect nodes,
        send "quit\n"
        expect Leeeeela
        exit 0
        """)
        val start = System.currentTimeMillis()
        system("expect $DIR/test.exp | tee $DIR/test.txt")
        val difftime = (System.currentTimeMillis() - start).toDouble()/1000.0

        val r = Regex("->")
        val line = readFile("test.txt").split("\n").filter{
            s -> r.containsMatchIn(s)
        }.first()
        val score = line.split("U: ")[1].split("%)")[0]
        val pv= line.let{
            var tmp = it
            correctALP.keys.forEach {
                key -> tmp = tmp.replace(key, correctALP[key]!!)
            }; tmp
        }.split("OV:").last().trim().split(" ")

        val eval = if (isblack)
            (score.toDouble() - 50) * 20 else -(score.toDouble() - 50) * 20
        val moves : Deque<String> = ArrayDeque<String>()
        val isblacks : Deque<Boolean> = ArrayDeque<Boolean>()
        var isblack1 = isblack
        pv.forEach { move ->
            if (move != "pass") {
                moves.add(move)
                isblacks.add(isblack1)
            }
            isblack1 = !isblack1
        }

        return Pair(lefttime - difftime, Branch(0,eval.toInt(),isblacks,moves))
    }

    fun ponder() {
        // ponder
        val genmap = HashMap<Int, Branch>()
        var sgf = "(;GM[1]SZ[$boardsize]KM[${if (isGote) "6.5" else "7.5"}]RU[Chinese]"
        var lefttime = 80.0
        var isblack = true
        var hand = 1

        val itb = Record.br.isblacks.iterator()
        val its = Record.br.moves.iterator()
        while (itb.hasNext()) {
            if (isblack != itb.next()) { // パス
                sgf += ";${if (isblack) "B" else "W"}[]"
                isblack = !isblack
            }
            val move = its.next()
            sgf += ";${if (isblack) "B" else "W"}["
            sgf += "${move[0].toLowerCase().toString() + f9a8b(move[1])}]"
            isblack = !isblack

            val (tmp, br) = gtp(sgf, lefttime, isblack)
            lefttime = tmp


            genmap[hand] = Branch(hand, br.eval, br.isblacks, br.moves)
            hand += 1
        }
        Genmove.genmap = genmap
        Genmove.isGenmove = true
    }

    fun search(){
        val (moves, isblacks) = Record.getNotation()

        var sgf = "(;GM[1]SZ[$boardsize]KM[${if (isGote) "6.5" else "7.5"}]RU[Chinese]RE[B+R]"
        val lefttime = 10.0
        val itb = isblacks.iterator()
        val its = moves.iterator()
        var isblack = true
        while (itb.hasNext()) {
            if (isblack != itb.next()) { // パス
                sgf += ";${if (isblack) "B" else "W"}[]"
                isblack = !isblack
            }
            val move = its.next()
            sgf += ";${if (isblack) "B" else "W"}["
            sgf += "${move[0].toLowerCase().toString() + f9a8b(move[1])}]"
            isblack = !isblack
        }
        val (_, br) = gtp(sgf, lefttime, isblack)

        Record.setSearch(br)
    }

    private fun f9a8b(c:Char) : String = (boardsize - c.toInt() + 145).toChar().toString()
    private fun readFile(file:String) = File(file).bufferedReader().use{it.readText()}
    private fun writeFile(file:String, str:String) = File(file).bufferedWriter().use{it.write(str)}
    private fun system(cmd: String) : Boolean {
        val commands = arrayOf("/bin/bash", "-c", cmd)
        val proc = Runtime.getRuntime().exec(commands)
        val stdInput = BufferedReader(InputStreamReader(proc.inputStream))
        val stdError = BufferedReader(InputStreamReader(proc.errorStream))
        for (s in stdInput.lines()) System.out.println(s)
        for (s in stdError.lines()) System.err.println(s)
        return proc.waitFor() == 0
    }
}