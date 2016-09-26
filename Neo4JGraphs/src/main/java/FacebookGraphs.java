/**
 * Created by Himangshu Ranjan Borah on 9/18/16.
 * Source Code for P2 : Social Computing CSC 555
 * Unity ID : hborah
 * Student ID : 200105222
 *
 * This code makes the SNAP Dataset Graphs in memory and calculates different Graph matrices.
 * It also Shows the analysis on the two proposed Hypothesis.
 */



import org.apache.commons.math.stat.correlation.PearsonsCorrelation;
import org.neo4j.graphalgo.CostEvaluator;
import org.neo4j.graphalgo.GraphAlgoFactory;
import org.neo4j.graphalgo.PathFinder;
import org.neo4j.graphalgo.impl.centrality.BetweennessCentrality;
import org.neo4j.graphalgo.impl.shortestpath.SingleSourceShortestPath;
import org.neo4j.graphalgo.impl.shortestpath.SingleSourceShortestPathDijkstra;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.io.fs.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

public class FacebookGraphs
{
    // The Database and the DataSet Directories.
    private static final File DB_PATH = new File( "target/neo4j-facebook-db" );
    private static final File DATA_PATH = new File("facebook");
    private GraphDatabaseService graphDb;
    private Node firstNode;
    private Node secondNode;
    private Relationship relationship;
    private long noOfNodes;
    private long noOfEdges;

    private enum RelTypes implements RelationshipType
    {
        KNOWS, KNOWS_EGO, IS_MEMBER_OF;
    }

    private enum LabelTypes implements Label
    {
        USER, GROUPS;
    }


    public static void main(String[] args) throws IOException
    {
        Integer[] userIds = {0, 107, 348, 414, 686, 698, 1684, 1912, 3437, 3980};
        List<Integer> userIdsList = Arrays.asList(userIds);
        HashSet<Integer> userIdsSet = new HashSet<Integer>(userIdsList);
        // Set the ID here for which you want to see the results.
        Integer currId = 0;// temp ID for testing. Later ro be used for all the IDs
        Scanner myScan = new Scanner(System.in);
        System.out.println("Please Enter the Ego Node ID you want to process or type ALL to run for all the Nodes in the Dataset ::");
        String userInput = myScan.next();
        if(userInput.equals("ALL")){
            for(Integer item:userIdsSet){
                FacebookGraphs thisClass = new FacebookGraphs();
                // Create the Graph DB out of the DataSets.
                thisClass.createDb(item);
                System.out.println(":::::The Graph No. of Nodes and Edges :::::");
                thisClass.printNodesEdges();//Must Call for the graph Size.
                System.out.println(":::::The Graph Betweenness Centralities :::::");
                thisClass.printBetweennessCentrality();
                System.out.println(":::::The Graph Clustering Coefficients :::::");
                thisClass.calculateAndPrintClusteringCoeffs();
                thisClass.proveHypothesisOne(10000);
                thisClass.proveHypothesisTwo(10000);
                //thisClass.testAPI();
                thisClass.shutDown();
            }

        } else if(userIdsSet.contains(Integer.parseInt(userInput))){
            currId = Integer.parseInt(userInput);
            FacebookGraphs thisClass = new FacebookGraphs();
            // Create the Graph DB out of the DataSets.
            thisClass.createDb(currId);
            System.out.println(":::::The Graph No. of Nodes and Edges :::::");
            thisClass.printNodesEdges();//Must Call for the graph Size.
            System.out.println(":::::The Graph Betweenness Centralities :::::");
            thisClass.printBetweennessCentrality();
            System.out.println(":::::The Graph Clustering Coefficients :::::");
            thisClass.calculateAndPrintClusteringCoeffs();
            thisClass.proveHypothesisOne(10000);
            thisClass.proveHypothesisTwo(10000);
            //thisClass.testAPI();
            thisClass.shutDown();
        } else {
            System.out.println("Invalid Input! Try Again.");
        }


    }

    private void testAPI(){
        int len = getShortestPathLengthSrcDest(graphDb.findNode(LabelTypes.USER, "ID", 6), graphDb.findNode(LabelTypes.USER, "ID", 3), 100);
        System.out.println("The Shortest path = " + len);
    }

    //this function prints the no. of Nodes and Edges
    private void printNodesEdges(){
        ResourceIterable<Node> itNodes = graphDb.getAllNodes();
        ResourceIterable<Relationship> itRel = graphDb.getAllRelationships();
        long nodeCount = 0;
        long edgeCount = 0;
        for(Node item:itNodes){
            /*if(item.hasProperty("CircleID"))
                System.out.println("Circle no of node = " + item.getProperty("ID") + " is " + item.getProperty("CircleID"));*/
            nodeCount++;
        }
        for(Relationship item:itRel){
            edgeCount++;
        }
        noOfNodes = nodeCount;
        noOfEdges = edgeCount;
        System.out.println("The no. of Nodes = " + nodeCount);
        System.out.println("The no. of Edges/Relationships(Taking the relationships between the altars twice for bi-direction) = " + (edgeCount));
    }

    //Calculate the clustering coefficient of all the nodes in the graph.
    private void calculateAndPrintClusteringCoeffs(){
        long[] rn;
        long R;
        long N;
        LinkedHashMap<Long, Double> clusterMap = new LinkedHashMap<Long, Double>();
        ResourceIterable<Node> itNodes = graphDb.getAllNodes();
        for(Node item:itNodes){
            long id = item.getId();
            rn = getRandN(id);
            R = rn[0];
            N = rn[1];
            double coeff = (double)(R)/(N * (N - 1));
            //double deno = calculateNeighborPermutation(N);
            //clusterMap.put(id, (double)R/deno);
            clusterMap.put(id, coeff);
        }
        System.out.println("The Clustering Coefficients of the Nodes are Below : mapped By Node IDs");
        for(Map.Entry<Long, Double> item : clusterMap.entrySet()){
            System.out.println("Node ID = " + graphDb.getNodeById(item.getKey()).getProperty("ID") + " :: Clustering Coefficient = " +
                    item.getValue());
        }
    }

    private int getShortestPathLengthSrcDest(Node src, Node dest, int maxDepth){
        PathFinder<Path> finder = GraphAlgoFactory.shortestPath(PathExpanders.forType(RelTypes.KNOWS), maxDepth);
        Path path = finder.findSinglePath(src, dest);
        if(path != null)
            return path.length();
        else return 0;
    }



    // Proving Hypothesis 2
    // <ssingh31>-<01>Shorter the path between two nodes, more likely they tend to be in the same circle.
    // Conversely, if we can show that the mean_average of the shortest paths between the people in same circles are less then we are done.
    // Steps : Select Random Samples.
    // Find the Shortest Distance Between the Two points.
    // Find if they belong to the same circle or not.
    // Maintain two lists, one for the same Circle Points, another for the different circle points.
    // Push to corresponding list.
    // Calculate the mean of both the lists. Compare.(Hypothesis test)

    private void proveHypothesisTwo(long noOfSamples){
        System.out.println("Testing the Second Hypothesis : <ssingh31>-<01>");
        Random rnd = new Random();
        ArrayList<Integer> seriesSameCircleDouble = new ArrayList<Integer>();
        ArrayList<Integer> seriesDiffCircleDouble = new ArrayList<Integer>();

        for(int i = 0; i< noOfSamples; i++) {
            //pick a pair.
            long firstNodeId = rnd.nextInt((int) noOfNodes);
            long secondNodeId = rnd.nextInt((int) noOfNodes);
            Node firstNode = graphDb.getNodeById(firstNodeId);
            Node secondNode = graphDb.getNodeById(secondNodeId);
            if (firstNode == null || secondNode == null) {
                System.out.println("NULL Node!");
                continue;
            }
            // Find the shortest path between the two nodes
            // 100 must be enough, considering 6 degrees of separation !
            int pathlen = getShortestPathLengthSrcDest(firstNode, secondNode, 100);
            String circle1;
            String circle2;
            if(firstNode.hasProperty("CircleID") && secondNode.hasProperty("CircleID")){
                circle1 = (String)firstNode.getProperty("CircleID");
                circle2 = (String)secondNode.getProperty("CircleID");
            } else {
                continue;
            }

            if(circle1.equals(circle2)){
                //same circle
                seriesSameCircleDouble.add(pathlen);
            }
            else {
                //different circle
                seriesDiffCircleDouble.add(pathlen);
            }
        }//main for

        //Analyse the two series.
        System.out.println("No of people in the same circle from this Sample = " + seriesSameCircleDouble.size());
        System.out.println("No of people in the different circle from this Sample = " + seriesDiffCircleDouble.size());
        long sameSum = 0;
        long diffSum = 0;

        for(Integer item : seriesSameCircleDouble){
            sameSum += item;
        }

        for(Integer item : seriesDiffCircleDouble){
            diffSum += item;
        }

        System.out.println("Sample Size = " + noOfSamples);
        System.out.println("The Average Shortest Path Length of People in Same Circle = "+ (double)sameSum/seriesSameCircleDouble.size());
        System.out.println("The Average Shortest Path Length of People in Different Circle = "+ (double)diffSum/seriesDiffCircleDouble.size());
        return;
    }


    /*
    Proving the Hypothesis 1
    <dding3>-<01>
    People who graduated from or are currently enrolled in the same university are more likely to connect with each other and form a social circle.
    No of samples specifies how many samples to draw from to come to a conclusion.
    */
    private void proveHypothesisOne(long noOfSamples){
        // Pick two nodes at random from the graph.
        // Find if they are equal in the Education : University fields. Call it Series A
        // Find if they are in the same circle. Call it Series B.
        // Find the Correlation between the Two Series A and B above
        System.out.println("Testing the First Hypothesis : <dding3>-<01>");
        Random rnd = new Random();
        boolean[] seriesA = new boolean[(int)noOfSamples];
        boolean[] seriesB = new boolean[(int)noOfSamples];
        double[] seriesADouble = new double[(int)noOfSamples];
        double[] seriesBDouble = new double[(int)noOfSamples];


        for(int i = 0; i< noOfSamples; i++){
            //pick a pair.
            long firstNodeId = rnd.nextInt((int)noOfNodes);
            long secondNodeId = rnd.nextInt((int)noOfNodes);
            Node firstNode = graphDb.getNodeById(firstNodeId);
            Node secondNode = graphDb.getNodeById(secondNodeId);
            if(firstNode == null || secondNode == null){
                System.out.println("NULL Node!");
                continue;
            }
            //find their education arrays.
            // 24 to 52 is the education;school;id;anonymized feature XX
            String[] firstVec;
            String[] secondVec;
            if(firstNode.hasProperty("FeatureVector") && secondNode.hasProperty("FeatureVector")){
                firstVec = (String[])firstNode.getProperty("FeatureVector");
                secondVec = (String[])secondNode.getProperty("FeatureVector");
            } else  {
                continue;
            }
            boolean isSameEducation = true;
            for(int j = 24; j <= 52; j++ ){
                if(firstVec[j] != secondVec[j])
                    isSameEducation = false;
            }


            //check if same circle.
            boolean isSameCircle = false;
            String circle1;
            String circle2;
            if(firstNode.hasProperty("CircleID") && secondNode.hasProperty("CircleID")){
                circle1 = (String)firstNode.getProperty("CircleID");
                circle2 = (String)secondNode.getProperty("CircleID");
            } else {
                continue;
            }

            if(circle1.equals(circle2))
                isSameCircle = true;
            else
                isSameCircle = false;


            seriesA[i] = isSameEducation;
            seriesB[i] = isSameCircle;

            if(seriesA[i] == true)
                seriesADouble[i] = 1;
            else
                seriesADouble[i] = 0;

            if(seriesB[i] == true)
                seriesBDouble[i] = 1;
            else
                seriesBDouble[i] = 0;

        }
        //Check correlation.
        int matchCountTrue = 0;
        int matchCountFalse = 0;
        for(int i = 0; i < seriesA.length; i++){
            if(seriesA[i] == seriesB[i]){
                if(seriesA[i] == true)
                    matchCountTrue++;
                else
                    matchCountFalse++;
            }
        }
        System.out.println("The True - True Matches in the SeriesA and B = " + matchCountTrue);
        System.out.println("The False - False Matches in the SeriesA and B = " + matchCountFalse);
        System.out.println("Total No of Samples = " + noOfSamples);
        System.out.println("Correlation Between the Double Series = " + new PearsonsCorrelation().correlation(seriesADouble, seriesBDouble));
        return;
    }



    private double calculateNeighborPermutation(long n){
        double result = 0.0d;
        //n!/(2!(n-2)!) = 4!/(2!(4-2)!)
        BigInteger numerator = factorial(n);
        BigInteger deno = factorial(2L).multiply(factorial(n - 2));
        result = numerator.doubleValue()/deno.doubleValue();
        return result;
    }

    //https://dzone.com/articles/finding-factorial-number-java
    private static BigInteger factorial(Long number){
        //Note that we have used BigInteger to store the factorial value.
        BigInteger factValue = BigInteger.ONE;

        for ( long i = 2; i <= number; i++){
            factValue = factValue.multiply(BigInteger.valueOf(i));
        }
        return factValue;
    }


    /*Reference : Neo4j website*/
    private static SingleSourceShortestPath<Double> getSingleSourceShortestPath() {
        return new SingleSourceShortestPathDijkstra<Double>( 0.0, null,
                new CostEvaluator<Double>()
                {
                    public Double getCost( Relationship relationship,
                                           Direction direction )
                    {
                        return 1.0;
                    }
                }, new org.neo4j.graphalgo.impl.util.DoubleAdder(),
                new org.neo4j.graphalgo.impl.util.DoubleComparator(),
                Direction.BOTH, RelTypes.KNOWS);
    }

    // Print Betweenness Centrality of every node using Dijkstra
    private void printBetweennessCentrality(){
        ResourceIterable<Node> allNodesItr = graphDb.getAllNodes();
        Set<Node> allNodes = new HashSet<Node>();
        for(Node item:allNodesItr){
            allNodes.add(item);
        }
        BetweennessCentrality<Double> betCentrality = new BetweennessCentrality<Double>(
                getSingleSourceShortestPath(), allNodes);
        betCentrality.calculate();

        for (Node item:allNodes){
            System.out.print("Betweenness Centrality of Node with ID: " + item.getProperty("ID") + " is = ");
            System.out.println(betCentrality.getCentrality(item));
        }
    }



    private static List<String> getFileDataInLines(String fileName) throws IOException{
        List<String> result = new LinkedList<String>();
        File fin = new File(DATA_PATH.getCanonicalPath() + File.separator + fileName);
        BufferedReader br = new BufferedReader(new FileReader(fin));

        String line = null;
        while ((line = br.readLine()) != null) {
            result.add(line);
        }
        br.close();
        return result;
    }

    private long[] getRandN(long nodeID){
        String query = "START a = node(" + nodeID + ") " +
                "MATCH (a)--(b) " +
                "WITH a, count(distinct b) as n " +
                "MATCH (a)--()-[r]-()--(a) " +
                "RETURN n, count(distinct r) as r";
        long[] retVals = new long[2];//0 = r, 1= n
        Result result = graphDb.execute(query);
        if(!result.hasNext()){
            //query failed
            retVals[0] = 0;
            retVals[1] = graphDb.getNodeById(nodeID).getDegree();
            return retVals;
        }
        try {
            Map<String, Object> row = result.next();
            retVals[0] = (Long)row.get("r");
            retVals[1] = (Long)row.get("n");
        } catch (Exception ex) {
            System.out.println("Error while executing Query in get R and N = " + ex.getMessage());
            ex.printStackTrace();
        }
        return retVals;
    }

    private Result getCypherExecuted(String query){
        Result result = graphDb.execute(query);
        if(!result.hasNext())
            return null;
        try {
            while ( result.hasNext() )
            {
                Map<String, Object> row = result.next();
                for ( String key : result.columns() )
                {
                    System.out.printf( "%s = %s%n", key, row.get( key ) );
                }
            }
        } catch (Exception ex) {
            System.out.println("Error while executing Query = " + ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }



    private void createDb(Integer currId) throws IOException
    {
        FileUtils.deleteRecursively( DB_PATH );
        graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( DB_PATH );
        registerShutdownHook( graphDb );
        // Generate the file names of the Datasets.
        String circlesFile = String.valueOf(currId) + ".circles";
        String edgesFile = String.valueOf(currId) + ".edges";
        String egofeatFile = String.valueOf(currId) + ".egofeat";
        String featFile = String.valueOf(currId) + ".feat";
        String featnamesFile = String.valueOf(currId) + ".featnames";

        System.out.println("Going to Create The Graph for EGO NODE = " + currId);
        //Get the EdgeList
        List<String> edgeList = getFileDataInLines(edgesFile);
        for (String item : edgeList) {
            String[] splittedNodes = item.split(" ");
            Integer firstNodeId = Integer.parseInt(splittedNodes[0]);
            Integer secondNodeId = Integer.parseInt(splittedNodes[1]);

            try
            {
                Transaction tx = graphDb.beginTx();
                //Search if the node is already there
                if(graphDb.findNode(LabelTypes.USER, "ID", firstNodeId) != null){
                    firstNode = graphDb.findNode(LabelTypes.USER, "ID", firstNodeId);
                } else {
                    firstNode = graphDb.createNode(LabelTypes.USER);
                    firstNode.setProperty("ID", firstNodeId);
                }

                if(graphDb.findNode(LabelTypes.USER, "ID", secondNodeId) != null){
                    secondNode = graphDb.findNode(LabelTypes.USER, "ID", secondNodeId);
                } else {
                    secondNode = graphDb.createNode(LabelTypes.USER);
                    secondNode.setProperty("ID", secondNodeId);
                }

                relationship = firstNode.createRelationshipTo(secondNode, RelTypes.KNOWS);
                //TODO: How to set the direction of th relationship? Currently adding both the Edges.

                //secondNode.createRelationshipTo(firstNode, RelTypes.KNOWS);
                tx.success();
            } catch (Exception e) {
                System.out.println("Transaction failed with issue = "+ e.getMessage());
                e.printStackTrace();
            }

        }
        //Add the connections from the ego node to all it's alters.
        try {
            Transaction tx = graphDb.beginTx();
            ResourceIterable<Node> alters = graphDb.getAllNodes();
            Node egoNode = graphDb.createNode(LabelTypes.USER);
            egoNode.setProperty("ID", currId);
            for(Node item:alters){
                //first node is egonode, second is item
                if(!egoNode.equals(item)){
                    egoNode.createRelationshipTo(item, RelTypes.KNOWS);
                    item.createRelationshipTo(egoNode, RelTypes.KNOWS);
                }
            }
            tx.success();
        } catch (Exception e) {
            System.out.println("Transaction failed while adding Ego with issue = "+ e.getMessage());
            e.printStackTrace();
        }

        //Add the circle information to the nodes.
        List<String> circlesList = getFileDataInLines(circlesFile);
        for(String line:circlesList){
            String[] splittedCircles = line.split("\t");
            String currentCircle = splittedCircles[0];
            //currID is the EDGO here. Att the circle to it first.
            graphDb.findNode(LabelTypes.USER, "ID", currId).setProperty("CircleID", currentCircle);
            for (int i = 1; i < splittedCircles.length; i++){
                Node temp = graphDb.findNode(LabelTypes.USER, "ID", Integer.parseInt(splittedCircles[i]));
                if(temp != null)
                    temp.setProperty("CircleID", currentCircle);
                else {
                    System.out.println("Found Node = " + splittedCircles[i] + " in Circle " + currentCircle + " which is not in the Graph!");
                }
            }
        }


        // Add the Feature Vectors.
        // First, add the EgoFeatures
        List<String> egoFeatList = getFileDataInLines(egofeatFile);
        String[] egoFeatSplitted = egoFeatList.get(0).split(" ");
        graphDb.findNode(LabelTypes.USER, "ID", currId).setProperty("FeatureVector", egoFeatSplitted);


        //Add the futures to the other nodes.
        List<String> featList = getFileDataInLines(featFile);
        for(String line : featList){
            String[] featSplitted = line.split(" ");
            int nodeId = Integer.parseInt(featSplitted[0]);
            String[] featVec = new String[featSplitted.length - 1];
            System.arraycopy(featSplitted, 1, featVec, 0, featSplitted.length - 1);
            Node temp = graphDb.findNode(LabelTypes.USER, "ID", nodeId);
            if(temp != null)
                temp.setProperty("FeatureVector", featVec);
            else
                System.out.println("Found Node = " + nodeId + " in Feat which is not in the Graph!");
        }

        return;
    }


    private void shutDown()
    {
        System.out.println();
        System.out.println( "Shutting down database ..." );
        graphDb.shutdown();
    }

    private static void registerShutdownHook( final GraphDatabaseService graphDb )
    {
        Runtime.getRuntime().addShutdownHook( new Thread()
        {
            @Override
            public void run()
            {
                graphDb.shutdown();
            }
        } );
    }
}
