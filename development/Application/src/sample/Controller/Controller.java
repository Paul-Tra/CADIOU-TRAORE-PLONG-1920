package sample.Controller;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import sample.Parser.GogolParser;
import sample.Parser.GraphmlParser;
import sample.Placement;
import sample.Relation;
import sample.Transaction;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    private final String GOGOLPATH = "./src/dependencies.gogol";
    public final double BOUND = 10;
    private String currentPath = "";
    private ArrayList<Transaction> transactions = new ArrayList<>();
    private ArrayList<Relation> relations = new ArrayList<>();
    private GogolParser gogolParser;
    @FXML
    public AnchorPane anchorPane1, anchorPane2, anchorPane3;
    @FXML
    public MenuBar menuBar;
    @FXML
    BorderPane borderPane1;
    @FXML
    public Label labelElement, labelElement2, labelSource, labelSource2,
            labelTarget, labelTarget2, labelDependencies, labelName,labelFileSource,
            labelFileSource2,labelFileTarget, labelFileTarget2;
    @FXML
    ListView listViewDependencies,listViewSourceLines,listViewTargetLines;
    @FXML
    Button buttonHide;

    @FXML
    private void clickButtonHide() {
        this.anchorPane2.setVisible(false);
        this.anchorPane3.setVisible(false);
    }
    @FXML
    private void clickListViewDependencies() {
        String dependency = (String)
                this.listViewDependencies.getSelectionModel().getSelectedItem();
        String source = this.labelSource2.getText();
        String target = this.labelTarget2.getText();
        ArrayList<String> sourceLines = new ArrayList<>();
        ArrayList<String> targetLines = new ArrayList<>();
        boolean conditional = false;
        if (this.labelDependencies.getText().contains("Conditional")) {
            conditional = true;
        }
        gogolParser.getDependencyLines(dependency, source, target, conditional,
                sourceLines, targetLines);
        System.out.println("sources :");
        sourceLines.forEach(s -> System.out.println(s));
        System.out.println("targets :");
        targetLines.forEach(s -> System.out.println(s));
        fillPopUp(sourceLines, targetLines);
    }

    /**
     * fills the elements of the Pop-up by source and target files information
     * @param sourceLines   lines causing a dependency in the source file
     * @param targetLines   lines causing a dependency in the target file
     */
    private void fillPopUp(ArrayList<String> sourceLines, ArrayList<String> targetLines) {
        clearPopUp();

        this.labelFileSource2.setText(this.labelSource2.getText() + ".sql");
        this.labelFileTarget2.setText(this.labelTarget2.getText() + ".sql");
        this.listViewSourceLines.getItems().addAll(sourceLines);
        this.listViewTargetLines.getItems().addAll(targetLines);


        this.anchorPane3.toFront();
        this.anchorPane3.setVisible(true);
    }

    private void clearPopUp() {
        this.listViewSourceLines.getItems().clear();
        this.listViewTargetLines.getItems().clear();
    }
    /**
     * manages the event when a Path is pressed in the anchorPane1
     */
    public void pressAnchorPane1Path(Relation relation) {
        System.out.println("pressAnchorPane1Path");
        this.anchorPane3.setVisible(false);
        this.anchorPane2.setVisible(true);
        this.anchorPane2.toFront();
        this.labelElement2.setText("Relation");
        this.labelSource2.setText(relation.getSource().getId());
        this.labelTarget2.setText(relation.getTarget().getId());
        if (relation.getArrow().getStrokeDashArray().isEmpty()) {
            this.labelDependencies.setText("Dependencies :");
        } else {
            this.labelDependencies.setText("Conditional Dependencies :");
        }
        ArrayList<String> dependencies = relation.getDependenciesLinesFromName();
        this.listViewDependencies.getItems().clear();
        this.listViewDependencies.getItems().addAll(dependencies);
        System.out.println("fin pressAnchorPane1Path");
    }

    @FXML
    private void onMenuItemClearLaunch() {
        if (this.currentPath.isEmpty() || this.currentPath.isBlank()) {
            return;
        }
        this.anchorPane2.setVisible(false);
        this.anchorPane1.getChildren().clear();

        // we put back the rampant elements after the clearing of anchorPane1
        this.anchorPane1.getChildren().addAll(this.labelName, this.anchorPane3);
        this.anchorPane3.setVisible(false);
        this.relations.clear();
        this.transactions.clear();
        generateGraph();
    }

    @FXML
    private void onMenuItemFolder(ActionEvent event) throws IOException {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select your folder");
        File defaultDirectory = new File("../../");
        chooser.setInitialDirectory(defaultDirectory);
        File s = chooser.showDialog(this.anchorPane1.getScene().getWindow());
        String c_dir = System.getProperty("user.dir") + "/src/";
        try {
            Runtime.getRuntime().exec("python3.7 " + s.getParent() + "/main.py " + s + "/");
            Runtime.getRuntime().exec("cp " + s.getParent() + "/graphs/graph.graphml " + c_dir);
            Runtime.getRuntime().exec("cp " + s.getParent() + "/graphs/dependencies.gogol " + c_dir);
            //System.out.println("./graph.graphml");
            this.currentPath = "src/graph.graphml";
            onMenuItemClearLaunch();
        } catch (Exception e) {
            System.out.println("issue causing by python3.7 execution");
        }
    }

    @FXML
    private void onMenuItemFile(ActionEvent event) {
        //Node node = (Node) event.getSource();
        FileChooser fil_chooser = new FileChooser();
        // add filters file's extension+
        fil_chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("graphml files", "*.graphml"));
        File file = fil_chooser.showOpenDialog(this.anchorPane1.getScene().getWindow());
        if (file != null) {
            this.currentPath = file.getAbsolutePath();
            onMenuItemClearLaunch();
        }
    }

    /**
     * looks after the generation of a Dependency graph from a graphml file
     * representing SQL queries
     */
    private void generateGraph() {
        if (this.currentPath.isEmpty() || this.currentPath.isBlank()) {
            return;
        }
        GraphmlParser graphmlParser = new GraphmlParser(this.currentPath);
        HashMap<Integer, String> map = graphmlParser.getTransactionMap();
        HashMap<Integer, String[]> map2 = graphmlParser.getRelationMap();


        fillListTransactionFromMap(map, this.transactions);
        fillListRelationFromMap(map2, this.relations, this.transactions);

        double boundWidth = this.anchorPane1.getWidth() -
                getMaxWidthTransactions(this.transactions) - Relation.loopSize;
        double boundHeight = this.anchorPane1.getHeight() - Relation.loopSize;
        Placement placement = new Placement(this.transactions, this.relations,
                boundWidth, boundHeight);
        placement.placementTransaction(this.transactions);
        this.relations.forEach(Relation::buildRelationShape);

        addRelationsToPane(this.relations);
        addTransactionsToPane(this.transactions);

        this.transactions.forEach((t) -> t.setController(this));
        this.relations.forEach((r) -> r.setController(this));

        this.gogolParser = new GogolParser(GOGOLPATH, this.relations);

    }


    public void hideControlCircles(Relation relation) {
        if (relation == null) {
            relations.forEach(relation1 -> {
                relation1.getControl1().setVisible(false);
                relation1.getControl2().setVisible(false);
            });
        } else {
            relations.forEach(relation1 -> {
                if (relation1 != relation) {
                    relation1.getControl1().setVisible(false);
                    relation1.getControl2().setVisible(false);
                }else{
                    relation.getControl1().toFront();
                    relation.getControl2().toFront();
                }
            });
        }
    }


    /**
     * identifies the bigger width of all Transaction's rectangle
     *
     * @param transactions all Transactions
     * @return the max value of Transaction's rectangle's width
     */
    private double getMaxWidthTransactions(ArrayList<Transaction> transactions) {
        double max = 0;
        for (Transaction transaction : transactions) {
            if (transaction.getRectangle().getWidth() > max) {
                max = transaction.getRectangle().getWidth();
            }
        }
        return max;
    }

    /**
     * Put Relations into the main Pane
     *
     * @param relations list of Relations that we want tu put into the main pane
     */
    private void addRelationsToPane(ArrayList<Relation> relations) {
        for (Relation relation : relations) {
            anchorPane1.getChildren().addAll(relation.getArrow(), relation.getEndArrow(),
                    relation.getControl1(), relation.getControl2());
        }
    }

    /**
     * Put Transactions into the main Pane
     *
     * @param transactions list of Transactions that we want tu put into the main pane
     */
    private void addTransactionsToPane(ArrayList<Transaction> transactions) {
        for (Transaction transaction : transactions) {
            anchorPane1.getChildren().addAll(transaction.getRectangle(),
                    transaction.getText());
        }
    }

    /**
     * Fills a list of Relations from a map
     *
     * @param map          map which contains all Relations information
     * @param relations    list of Relations that we want to fill
     * @param transactions list of Transaction use to the Transactions of a Relation
     */
    private void fillListRelationFromMap(HashMap<Integer, String[]> map,
                                         ArrayList<Relation> relations,
                                         ArrayList<Transaction> transactions) {
        for (int i = 0; i < map.size(); i++) {
            String content[] = map.get(i);

            Relation r = new Relation(getTransactionFromId(transactions, content[0]),
                    getTransactionFromId(transactions, content[1]), content[2], content[3]);
            relations.add(r);
        }
    }

    /**
     * Finds a Transaction into a list of Transaction by its id
     *
     * @param transactions list of Transactions where we searched a Transaction
     * @param id           id of the searched Transaction
     * @return the transaction with the same id as id parameter
     */
    private Transaction getTransactionFromId(ArrayList<Transaction> transactions,
                                             String id) {
        for (Transaction transaction : transactions) {
            if (transaction.getId().equals(id)) {
                return transaction;
            }
        }
        return null;
    }

    /**
     * Fills a list of Transactions from a map
     *
     * @param map          map which contains all Transactions information
     * @param transactions list of Transactions that we want to fill
     */
    private void fillListTransactionFromMap(HashMap<Integer, String> map,
                                            ArrayList<Transaction> transactions) {
        for (int i = 0; i < map.size(); i++) {
            String content = map.get(i);
            Transaction t = new Transaction(content);
            transactions.add(t);
        }
    }


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        /* TODO : should be false at the very beginning of the use */
        anchorPane2.setVisible(false);
        labelElement2.setText("no element selected");
        labelSource.setText("Source :");
        labelSource2.setText("no source found");
        labelTarget.setText("Target :");
        labelTarget2.setText("no target found");
        labelDependencies.setText("Dependencies");
        listViewDependencies.setStyle("-fx-font-size : 11");
        labelName.setVisible(false);
        this.listViewSourceLines.setStyle("-fx-font-size : 11");

        this.listViewTargetLines.setStyle("-fx-font-size : 11");
        labelName.toFront();
        this.anchorPane3.setVisible(false);
    }
}