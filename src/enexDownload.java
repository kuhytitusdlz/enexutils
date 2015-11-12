import com.evernote.auth.EvernoteAuth;
import com.evernote.auth.EvernoteService;
import com.evernote.clients.ClientFactory;
import com.evernote.clients.NoteStoreClient;
import com.evernote.clients.UserStoreClient;
import com.evernote.edam.error.EDAMErrorCode;
import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMUserException;
import java.io.File;

import com.evernote.edam.notestore.NoteFilter;
import com.evernote.edam.notestore.NoteList;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.NoteSortOrder;
import com.evernote.edam.type.Resource;
import com.evernote.edam.type.Tag;
import com.evernote.thrift.transport.TTransportException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Iterator;
import java.util.Scanner;
import java.util.TimeZone;

/**
 *
 * @author dadits
 */
public class enexDownload {

    private UserStoreClient userStore;
    private NoteStoreClient noteStore;

    public static void main(String[] argv) throws Exception {
        if (argv.length == 0 || (argv.length == 1 && argv[0].equals("-help"))) {
            System.out.println("\nUsage:  java enexDownload AUTH_TOKEN filterNotes downloadFolder PRODUCTION");
            System.out.println("   where    AUTH_TOKEN is authentication token (To get a token, go to https://sandbox.evernote.com/api/DeveloperToken.action or https://www.evernote.com/api/DeveloperToken.action)");
            System.out.println("            filterNotes Searches are formatted according to the Evernote search grammar (learn more at http://dev.evernote.com/documentation/cloud/chapters/Searching_notes.php");
            System.out.println("            downloadFolder folder to download .enex files");
            System.out.println("            PRODUCTION flag, or SANDBOX");
            System.out.println("   Sample:  java -classpath evernote-api-1.25.1.jar:jdom-2.0.6.jar:. enexDownload \"S=s9:U=107980:E=157a13fae5b:C=150499e8140:P=1cd:A=en-devtoken:V=2:H=8c61a7d252056789538e32dbd65f400b\" \"tag:kindle created:day-60\" \"./enex\" PRODUCTION");
            System.exit(1);
        }

        //query = "tag:kindle created:day-60";
        //folderPath = "./enex";
        String token, query, folderPath, prodFlag;
        if (argv[0] != null) {
            token = argv[0].replace("\"", "");
        } else {
            System.err.println("Please fill in your developer token");
            System.err.println("To get a developer token, go to https://sandbox.evernote.com/api/DeveloperToken.action or https://www.evernote.com/api/DeveloperToken.action");
            return;
        }

        // Searches are formatted according to the Evernote search grammar.
        // Learn more at
        // http://dev.evernote.com/documentation/cloud/chapters/Searching_notes.php
        // To search for notes with a specific tag, we could do something like
        // this:
        // String query = "tag:tagname";
        // To search for all notes with the word "elephant" anywhere in them:
        // String query = "elephant";
        // In this example, we search for notes that have the term "EDAMDemo" in
        // the title.
        // This should return the sample note that we created in this demo app.
        //String query = "intitle:EDAMDemo";
        query = argv[1].replace("\"", "");
        folderPath = argv[2].replace("\"", "");
        if (argv[3] != null) {
            prodFlag = argv[3].replace("\"", "");
        } else {
            prodFlag = "SANDBOX";
        }

        System.out.println("query = " + query);
        System.out.println("folderPath = " + folderPath);

        enexDownload demo = new enexDownload(token, prodFlag);
        try {
            demo.enexGet(query, folderPath);
        } catch (EDAMUserException e) {
            // These are the most common error types that you'll need to
            // handle
            // EDAMUserException is thrown when an API call fails because a
            // paramter was invalid.
            if (e.getErrorCode() == EDAMErrorCode.AUTH_EXPIRED) {
                System.err.println("Your authentication token is expired!");
            } else if (e.getErrorCode() == EDAMErrorCode.INVALID_AUTH) {
                System.err.println("Your authentication token is invalid!");
            } else if (e.getErrorCode() == EDAMErrorCode.QUOTA_REACHED) {
                System.err.println("Your authentication token is invalid!");
            } else {
                System.err.println("Error: " + e.getErrorCode().toString()
                        + " parameter: " + e.getParameter());
            }
        } catch (EDAMSystemException e) {
            System.err.println("System error: " + e.getErrorCode().toString());
        } catch (TTransportException t) {
            System.err.println("Networking error: " + t.getMessage());
        }

    }

    /**
     * Intialize UserStore and NoteStore clients. During this step, we authenticate with the
     * Evernote web service. All of this code is boilerplate - you can copy it straight into your
     * application.
     */
    public enexDownload(String token, String prodFlag) throws Exception {
        EvernoteAuth evernoteAuth;

        System.out.println("token = " + token);
        // Set up the UserStore client and check that we can speak to the server
        if (prodFlag.compareToIgnoreCase("PRODUCTION") == 0) {
            System.out.println("prodFlag = " + prodFlag);
            evernoteAuth = new EvernoteAuth(EvernoteService.PRODUCTION, token);
        } else {
            System.out.println("prodFlag = " + "SANDBOX");
            evernoteAuth = new EvernoteAuth(EvernoteService.SANDBOX, token);
        }

        ClientFactory factory = new ClientFactory(evernoteAuth);
        userStore = factory.createUserStoreClient();

        boolean versionOk = userStore.checkVersion("Evernote EDAMDemo (Java)",
                com.evernote.edam.userstore.Constants.EDAM_VERSION_MAJOR,
                com.evernote.edam.userstore.Constants.EDAM_VERSION_MINOR);
        if (!versionOk) {
            System.err.println("Incompatible Evernote client protocol version");
            System.exit(1);
        }

        // Set up the NoteStore client
        noteStore = factory.createNoteStoreClient();
    }

    public void enexGet(String query, String folderPath) throws Exception {
        NoteFilter filter = new NoteFilter();
        filter.setWords(query);
        filter.setOrder(NoteSortOrder.UPDATED.getValue());
        filter.setAscending(false);

        // Find the first 50 notes matching the search
        System.out.println("Searching for notes matching query: " + query);
        NoteList notes = noteStore.findNotes(filter, 0, 50);
        System.out.println("Found " + notes.getTotalNotes() + " matching notes");

        Iterator<Note> iter = notes.getNotesIterator();
        while (iter.hasNext()) {
            Note note = iter.next();
            System.out.println("===================== Note: " + note.getTitle());

            // Note objects returned by findNotes() only contain note attributes
            // such as title, GUID, creation date, update date, etc. The note
            // content
            // and binary resource data are omitted, although resource metadata
            // is included.
            // To get the note content and/or binary resources, call getNote()
            // using the note's GUID.
            //Note fullNote = noteStore.getNote(note.getGuid(), true, true, false, false);
            //Note fullNote = noteStore.getNote(note.getGuid(), true, true, false, false);
            // скачиваем заметку без контента
            Note fullNote = noteStore.getNote(note.getGuid(), false, false, false, false);
            if (new File("enex-download-history.csv").isFile() && searchUsingScanner(fullNote.getGuid(), "enex-download-history.csv") != null) {
                continue; // если такой файл уже скачивали, то читаем следующую заметку
            }
            // скачиваем заметку полностью        
            fullNote = noteStore.getNote(note.getGuid(), true, true, false, false);
            writeToFile(fullNote.getGuid() + ";" + fullNote.getTitle(), "enex-download-history.csv"); // ведем лог скаченных заметок, чтобы не сачивать повторно

            String head, tail, tags, resources, resourceBody, enex, filePath;
            tags = "";
            resourceBody = "";
            resources = "";

            for (String tagGuid : fullNote.getTagGuids()) {
                Tag tag = noteStore.getTag(tagGuid);
                tags = tags + "    <tag>" + tag.getName() + "</tag>\n";
            }

            head = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<!DOCTYPE en-export SYSTEM \"http://xml.evernote.com/pub/evernote-export2.dtd\">\n"
                    + "<en-export export-date=\"" + dateISO8601(new Date()) + "\" application=\"EDAMDemo app\">\n"
                    + "  <note>\n"
                    + "    <title>" + fullNote.getTitle() + "</title>\n"
                    + "    <created>" + dateISO8601(new Date()) + "</created>\n" + tags;
            head = head + "    <note-attributes>\n";
            if (fullNote.getAttributes().isSetSource()) {
                head = head + "      <source>" + fullNote.getAttributes().getSource() + "</source>\n";
            } else {
                head = head + "      <source>Clearly</source>\n";
            }
            if (fullNote.getAttributes().isSetSourceURL()) {
                head = head + "      <source-url>" + fullNote.getAttributes().getSourceURL() + "</source-url>\n";
            }
            if (fullNote.getAttributes().isSetSourceApplication()) {
                head = head + "      <source-application>" + fullNote.getAttributes().getSourceApplication() + "</source-application>\n";
            }
            head = head + "    </note-attributes>";
            if (fullNote.getResourcesSize() > 0) {
                for (Resource resource : fullNote.getResources()) {
                    resources = resources + "    <mime>" + resource.getMime() + "</mime>\n"
                            + "    <resource>\n";
                    if (resource.isSetWidth()) {
                        resources = resources + "      <width>" + resource.getWidth() + "</width>\n";
                    }
                    if (resource.isSetHeight()) {
                        resources = resources + "      <height>" + resource.getHeight() + "</height>\n";
                    }
                    if (resource.isSetDuration()) {
                        resources = resources + "      <duration>" + resource.getDuration() + "</duration>\n";
                    }
                    //if (resource.isSetRecognition()) {
                    //    resources = resources + "      <recognition>" + resource.getRecognition() + "</recognition>\n";
                    //}

                    if (resource.isSetAttributes()) {
                        resources = resources + "      <resource-attributes>\n";
                        if (resource.getAttributes().isSetSourceURL()) {
                            resources = resources + "        <source-url>" + resource.getAttributes().getSourceURL() + "</source-url>\n";
                        }
                        if (resource.getAttributes().isSetTimestamp()) {
                            resources = resources + "        <timestamp>" + resource.getAttributes().getSourceURL() + "</timestamp>\n";
                        }
                        if (resource.getAttributes().isSetLatitude()) {
                            resources = resources + "        <latitude>" + resource.getAttributes().getLatitude() + "</latitude>\n";
                        }
                        if (resource.getAttributes().isSetLongitude()) {
                            resources = resources + "        <longitude>" + resource.getAttributes().getLongitude() + "</longitude>\n";
                        }
                        if (resource.getAttributes().isSetAltitude()) {
                            resources = resources + "        <altitude>" + resource.getAttributes().getAltitude() + "</altitude>\n";
                        }
                        if (resource.getAttributes().isSetSourceURL()) {
                            resources = resources + "        <camera-make>" + resource.getAttributes().getSourceURL() + "</camera-make>\n";
                        }
                        if (resource.getAttributes().isSetCameraMake()) {
                            resources = resources + "        <camera-model>" + resource.getAttributes().getCameraMake() + "</camera-model>\n";
                        }
                        if (resource.getAttributes().isSetRecoType()) {
                            resources = resources + "        <reco-type>" + resource.getAttributes().getRecoType() + "</reco-type>\n";
                        }
                        if (resource.getAttributes().isSetFileName()) {
                            resources = resources + "        <file-name>" + resource.getAttributes().getFileName() + "</file-name>\n";
                        }
                        if (resource.getAttributes().isSetAttachment()) {
                            resources = resources + "        <attachment>" + "???????????????????" + "</attachment>\n";
                        }
                        if (resource.getAttributes().isSetApplicationData()) {
                            resources = resources + "        <application-data>" + resource.getAttributes().getApplicationData() + "</application-data>\n";
                        }
                        resources = resources + "      </resource-attributes>\n";
                    }
                    resources = resources + "      <data encoding=\"base64\">\n";
                    resourceBody = Base64.getEncoder().encodeToString(resource.getData().getBody());
                    // Default length of base64 output lines
                    for (int i = 75; i < resourceBody.length(); i = i + 76) {
                        resourceBody = new StringBuffer(resourceBody).insert(i, "\n").toString();
                    }
                    resources = resources + resourceBody;
                    resources = resources + "\n      </data>\n" + "    </resource>\n";
                }
            }

            tail = "  </note>\n" + "</en-export>";

            enex = head + "\n    <content><![CDATA[" + fullNote.getContent() + "]]></content>\n" + resources + tail;
            filePath = note.getTitle();
            filePath = filePath.replace("/", "");
            filePath = filePath.replace(":", "");
            filePath = filePath.replace("?", "");

            filePath = folderPath + "/" + filePath + ".enex";

            System.out.println("Create file: " + filePath);
            //System.out.println(enex);
            writeToFile(enex, filePath);
            //System.out.println(fullNote.getResources().get(0).getData().getBodyHash());
            //System.out.println(noteStore.getNoteContent(note.getGuid()));
            System.out.println();
        }
    }

    private void writeToFile(String str, String path) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(path, true)); // true - для дописывания, а не перезатирания
        out.write(str);
        out.newLine();
        out.close();
    }

    private String dateISO8601(Date date) throws Exception {
        // get date from long to ISO 8601
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        df.setTimeZone(tz);
        return df.format(date);
    }

    public static String searchUsingScanner(String searchQuery, String filePath) throws FileNotFoundException {
        searchQuery = searchQuery.trim();
        Scanner scanner = null;
        try {
            scanner = new Scanner(new File(filePath));
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.contains(searchQuery)) {
                    return line;
                } else {
                }
            }
        } finally {
            try {
                if (scanner != null) {
                    scanner.close();
                }
            } catch (Exception e) {
                System.err.println("Exception while closing scanner " + e.toString());
            }
        }

        return null;
    }

    public static String searchUsingBufferedReader(String filePath, String searchQuery) throws IOException {
        searchQuery = searchQuery.trim();
        BufferedReader br = null;

        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath)));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains(searchQuery)) {
                    return line;
                } else {
                }
            }
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (Exception e) {
                System.err.println("Exception while closing bufferedreader " + e.toString());
            }
        }

        return null;
    }
}
