import java.io.*;
import java.util.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
// import org.apache.lucene.util.ToStringUtils;s
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.PostingsEnum;


public class indexing {
    public static void main(String[] args) throws IOException {
        docSaver();
        // Input folder
        String docsPath = "inputFiles";

        // Output folder
        String indexPath = "indexedFiles";

        // Input Path Variable
        final Path docDir = Paths.get(docsPath);

        try {
            // org.apache.lucene.store.Directory instance
            Directory dir = FSDirectory.open(Paths.get(indexPath));

            // analyzer with the default stop words
            Analyzer analyzer = new StandardAnalyzer();

            // IndexWriter Configuration
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);

            // IndexWriter writes new index files to the directory
            IndexWriter writer = new IndexWriter(dir, iwc);

            // Its recursive method to iterate all files and directories
            indexDocs(writer, docDir);

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        termFrequency("hi");
    }

    // This method seperates the group's documents from the whole dataset based on
    // group number
    static String[] docSplitter(int groupNum, String text) {
        int begin = (groupNum - 1) * 103 + 1;
        int end = groupNum * 103 + 1;
        String[] docs = text.split("\\.I\\s\\d+\\s\\.W\\s");
        docs = Arrays.copyOfRange(docs, begin, end);
        return docs;
    }

    // save docs to different .txt file in input files
    static void docSaver() {
        try {
            String text = Files.readString(Path.of("lucene_ dataset.txt"));
            int gpNum = 5;
            String[] docs = docSplitter(gpNum, text);
            int i = 0;
            for (String doc : docs) {
                String name = "inputFiles/data" + Integer.toString(i++) + ".txt";
                txtSaver(name, doc);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
    // save content to .txt file in given path
    static void txtSaver(String path,String content){
        try{
            PrintWriter out = new PrintWriter(path);
            out.println(content);
            out.close();
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
    }

    static void indexDocs(final IndexWriter writer, Path path) throws IOException {
        // Directory?
        if (Files.isDirectory(path)) {
            // Iterate directory

            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                // Map<Integer , String> mapToReal = new HashMap<Integer, String>();
                // int k = 0;
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    try {
                        // Index this file
                        // System.out.println("hi->");
                        // mapToReal.put(k , file.toString());
                        indexDoc(writer, file, attrs.lastModifiedTime().toMillis());
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            // Index this file
            indexDoc(writer, path, Files.getLastModifiedTime(path).toMillis());
        }
    }

    static void indexDoc(IndexWriter writer, Path file, long lastModified) throws IOException {
        try (InputStream stream = Files.newInputStream(file)) {
            // Create lucene Document
            Document doc = new Document();
            doc.add(new StringField("path", file.toString(), Field.Store.YES));
            FieldType myFieldType = new FieldType(TextField.TYPE_STORED);
            myFieldType.setStoreTermVectors(true);
            doc.add(new Field("contents", new String(Files.readAllBytes(file)), myFieldType));
            
            // Updates a document by first deleting the document(s)
            // containing <code>term</code> and then adding the new
            // document. The delete and then add are atomic as seen
            // by a reader on the same index
            writer.updateDocument(new Term("path", file.toString()), doc);
        }
    }

    //find term frequency for each term in each document and save result csv file
    static void termFrequency(String indexPath){

        List<Map<String, Long>> listofTF = new ArrayList<Map<String, Long>>();
        Map<String, List<Long>> docPerTerm = new HashMap<String,List<Long>>();

        for(int i = 0; i < 103 ; i++){
            Map<String,Long> tf = new HashMap<String,Long>();
            try{
                IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get("indexedFiles")));
                Terms termVector = reader.getTermVector(i , "contents");
                TermsEnum itr = termVector.iterator();
                BytesRef term = null;
                PostingsEnum postings = null;

                while((term = itr.next()) != null){

                    String termText = term.utf8ToString();
                    postings = itr.postings(postings, PostingsEnum.FREQS);
                    postings.nextDoc();
                    long termFreq = postings.freq();

                    tf.put(termText, termFreq);

                    if(docPerTerm.containsKey(termText)){
                        List<Long> current = docPerTerm.get(termText);
                        current.add(new Long(i));
                        docPerTerm.put(termText , current);
                    }
                    else{
                        List<Long> current = new ArrayList<Long>();
                        current.add(new Long(i));
                        docPerTerm.put(termText , current);
                    }
                    // System.out.println("term: "+termText+", termFreq = "+termFreq);
                }
                String path = "result/document" + Integer.toString(i) + ".txt";
                txtSaver(path, tf.toString());
                listofTF.add(tf);
                reader.close();

            }catch(IOException ioe){
                ioe.printStackTrace();
            }
        }
        String path = "result/DocPerTerm.txt";
        txtSaver(path, docPerTerm.toString());
        // for(int i = 0 ; i < 102 ; i++){
        //     System.out.println("----------------------------------------------->DOCUMET" + i);
        //     for(Map.Entry<String,Long> entry : listofTF.get(i).entrySet()){
        //         System.out.println(entry.getKey() + entry.getValue());

        //     }
        // }
    }

        
}
