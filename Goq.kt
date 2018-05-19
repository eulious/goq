import javafx.application.Application
import javafx.event.ActionEvent
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage

import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.canvas.Canvas
import javafx.scene.control.*
import java.net.URL
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.input.KeyEvent
import java.util.ResourceBundle
import javafx.scene.layout.AnchorPane

fun main(args: Array<String>) {
    Application.launch(MyApplication::class.java, *args)
}

class MyApplication : Application() {
    override fun start(stage: Stage) {
        stage.title = "Hello World"
        stage.scene = Scene(FXMLLoader.load(javaClass.getResource("FXMLDocument.fxml")))
        stage.show()
    }
}

class Controller : Initializable {
    @FXML lateinit var canvas: Canvas
    @FXML lateinit var graph: Canvas
    @FXML lateinit var pane: AnchorPane
    @FXML lateinit var leaf: TableView<Leaf>
    @FXML lateinit var handcol: TableColumn<Any, Any>
    @FXML lateinit var pvcol: TableColumn<Any, Any>
    @FXML lateinit var colorcol: TableColumn<Any, Any>
    @FXML lateinit var genlabel: Label
    @FXML lateinit var tree: TreeView<String>

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        handcol.cellValueFactory = PropertyValueFactory("handdisp");
        pvcol.cellValueFactory = PropertyValueFactory("pvdisp");
        colorcol.cellValueFactory = PropertyValueFactory("colordisp");

        pane.setOnScroll { e->
            Record.next(e.deltaY < 0)
        }

        pane.setOnKeyPressed { e: KeyEvent ->
            println(e.code)
            when {
                e.code.toString() == "BACK_SPACE" -> Tree.delete()
                e.code.toString() == "UP" -> Record.next(true)
                e.code.toString() == "DOWN" -> Record.next(false)
            }
        }

        canvas.setOnMousePressed { e ->
            Record.add(Board.getClickedsign(
                    e.sceneX - canvas.layoutX,
                    e.sceneY - canvas.layoutY
            ))
        }

        Tree.initTree(tree)
        Board.initCanvas(canvas)
        Table.initTable(leaf)
        Genmove.initGenmove(graph, genlabel)
    }

    @FXML fun handlePasteButton(e: ActionEvent){
        println("handlePasteButton")
        Ponder.paste()
    }
    @FXML fun handlePonderButton(e: ActionEvent){
        println("handlePonderButton")
        Ponder.ponder()
    }
    @FXML fun handleNewButton(e: ActionEvent){
        println("handlePasteButton")
        Ponder.search()
    }
}
