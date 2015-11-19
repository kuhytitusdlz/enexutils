
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.ElementFilter;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author dadits
 */
public class enexToHtml {

    public static void main(String[] argv) throws Exception {
        if (argv.length == 0 || (argv.length == 1 && argv[0].equals("-help"))) {
            System.out.println("\nUsage:  java enexToHtml EnexFiles");
            System.out.println("   where EnexFilesDirectory is enex file or folder with enex files");
            System.out.println("   Sample:  java -classpath jdom-2.0.6.jar:. enexToHtml \"./enex\"");
            System.exit(1);
        }

        if (true) {
            enexToHtml bla = new enexToHtml();
            File fPath = new File(argv[0]);
            if (fPath.isFile()) {
                if (fPath.getName().endsWith(".enex")) {
                    bla.enexToHtml(null, argv[0]);
                }
            } else if (fPath.isDirectory()) {
                File[] listOfFiles = fPath.listFiles();
                for (int i = 0; i < listOfFiles.length; i++) {
                    if (listOfFiles[i].isFile()) {
                        if (listOfFiles[i].getName().endsWith(".enex")) {
                            bla.enexToHtml(fPath.getName(), listOfFiles[i].getName());
                        }
                    }
                }
            }
        }
    }

    public void enexToHtml(String folder, String file) throws NoSuchAlgorithmException, URISyntaxException {
        if (folder != null && new File(folder + "/" + file.replace(".enex", ".html")).isFile()) {
            System.out.println("File exist: " + file.replace(".enex", ".html"));
            return; // если файл html уже есть, то выходим
        }

        SAXBuilder sb = new SAXBuilder();
        Document docEnex, docHtml;
        String title, content, strData, folderPath, strTemp, enmediahash;
        // получаем "красивый" формат для вывода XML
        Format fmt = Format.getPrettyFormat();
        XMLOutputter xo = new XMLOutputter(fmt);
        //Map<String, String> hashmap = new HashMap<String, String>(); // парная таблица соответствия: хэш файла - путь до файла
        HashMap hashmap = new HashMap();

        try {

            docEnex = sb.build(new File(folder + "/" + file));

            // получаем список элементов note (их всего один, но такие уж правила)
            List note_elements = docEnex.getRootElement().getContent(new ElementFilter("note"));
            if (note_elements.size() > 1) {
                // если элементов note больше 1, то это странно
                System.err.println("Note nodes = " + note_elements.size() + " > 1, exit(1)");
                System.exit(1);
            }
            Iterator iterator = note_elements.iterator();
            while (iterator.hasNext()) {
                Element node = (Element) iterator.next();
                //System.out.println("note.getName() : " + node.getName());
                title = node.getChildText("title");
                content = node.getChildText("content");
                //content  = "<html><body>" + content + "</body></html>";
                //writeToFile(content, title + ".html");

                strData = node.getChildText("file-name");

//                List resource = node.getChildren("resource");
//                Iterator resourceIterator = resource.iterator();
//                while (resourceIterator.hasNext()) {
//                    Element child = (Element) resourceIterator.next();
//                    System.out.println(child.getChild("resource-attributes").getChildText("file-name"));
//                }
                // из-за этого DOCTYPE все тормозит
                content = content.replace("<!DOCTYPE en-note SYSTEM \"http://xml.evernote.com/pub/enml2.dtd\">", "");
                // получить content как XML объект
                docHtml = sb.build(new StringReader(content));
                // находим все en-media
                //System.out.println(xo.outputString(docHtml));
                //List enmedia_elements = docHtml.getRootElement().getContent(new ElementFilter("en-media"));
                Iterator<?> enmedia_elements = docHtml.getRootElement().getDescendants(new ElementFilter("en-media"));
                while (enmedia_elements.hasNext()) {
                    folderPath = folder + "/" + file.replace(".enex", "_files");
                    new File(folderPath).mkdir();

                    List resource_elements = node.getContent(new ElementFilter("resource")); // находим все ресурсы
                    Iterator resource_iterator = resource_elements.iterator();
                    while (resource_iterator.hasNext()) {
                        Element resource_node = (Element) resource_iterator.next();
                        byte[] data;
                        strData = resource_node.getChildText("data").replaceAll("\\s",""); // удалить все пробелы
                        if (strData.substring(strData.length() - 1).equals("=")) // удалить padding '=' если есть в конце
                        {
                            strData = strData.substring(0, strData.length() - 1); // удалить последний символ
                            if (strData.substring(strData.length() - 1).equals("=")) // если есть еще один padding '=', то и его удаляем
                            {
                                strData = strData.substring(0, strData.length() - 1); // удалить последний символ
                            }
                        }
                        data = Base64.getMimeDecoder().decode(strData);
                        if (resource_node.getChild("resource-attributes").getChild("file-name") != null) {
                            strTemp = resource_node.getChild("resource-attributes").getChildText("file-name");
                        } else if (resource_node.getChild("resource-attributes").getChild("source-url") != null) {
                            URI uri = new URI(resource_node.getChild("resource-attributes").getChildText("source-url"));
                            String path = uri.getPath();
                            strTemp = path.substring(path.lastIndexOf('/') + 1);
                        } else {
                            strTemp = null;
                            System.err.println("Note nodes = " + note_elements.size() + " > 1, exit(1)");
                            System.exit(1);
                        }
                        // имя выходного файла
                        try (OutputStream stream = new FileOutputStream(folderPath + "/" + strTemp)) {
                            stream.write(data); // записать все en-media объекты в файлы
                        }
                        MessageDigest md = MessageDigest.getInstance("MD5");
                        byte[] hash_b = md.digest(data); // посчитать MD5 объекта
                        //System.out.println("hash = " + new BigInteger(1, hash_b).toString(16));
                        String strTemp2;
                        strTemp2 = new BigInteger(1, hash_b).toString(16);
                        if (strTemp2.length() == 31) {
                            strTemp2 = "0" + strTemp2; // если первый 0, то почему-то обрезает, добавляем
                        }
                        hashmap.put(strTemp2, file.replace(".enex", "_files") + "/" + strTemp);
                    }
                    //Iterator enmiterator = enmedia_elements.iterator();
                    //while (enmiterator.hasNext()) {
                    Element emnode = (Element) enmedia_elements.next();
                    //System.out.println("emnode.getName() = " + emnode.getName());
                    //System.out.println("emnode.getAttributeValue(\"hash\") = " + emnode.getAttributeValue("hash"));
                    //System.out.println("emnode.getAttributeValue(\"type\") = " + emnode.getAttributeValue("type"));
                    // для каждого найденного en-media даем ссылку на него
                    if (emnode.getAttributeValue("hash") != null) {
                        if (hashmap.get(emnode.getAttributeValue("hash")) != null) {
                            enmediahash = hashmap.get(emnode.getAttributeValue("hash")).toString();
                            if (!enmediahash.equals("")) {
                                emnode.setAttribute("src", enmediahash);
                            }
                        }
                    }

                    // удаляем ненужный атрибут hash
                    emnode.removeAttribute("hash");
                    emnode.setName("img");
                    //}
                }
                //System.out.println(xo.outputString(docHtml));
                strTemp = "<html>\n"
                        + "<head>\n"
                        + "  <meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\" />\n"
                        + "</head>" + xo.outputString(docHtml) + "\n</html>";
                writeToFile(strTemp, folder + "/" + file.replace(".enex", ".html"));

                //System.out.println("title = " + title);
                //System.out.println("content = " + content);
                //System.out.println("file-name = " + strData);
                //String id = node.getAttributeValue("id");
                //String department = node.getChildText("tag");
                //System.out.println(id + ": " + title + " - " + department);
            }
            //Filter filter = new ContentFilter(ContentFilter.CDATA);

            Element rootNode = docEnex.getRootElement();

            List list = rootNode.getChildren("note");
            //System.out.println("list.size() = " + list.size());

            for (int i = 0; i < list.size(); i++) {

                Element node = (Element) list.get(i);
                //System.out.println("node.getContent() : " + node.getContent());
                //System.out.println("node.getName() : " + node.getName());
                //System.out.println("node.getText() : " + node.getText());

            }
            //xo.setTrimAllWhite(true);
            //xo.output(docEnex, System.out);
        } catch (JDOMException | IOException e) {
        }
    }

    private void writeToFile(String str, String path) throws IOException {
        BufferedWriter out = new BufferedWriter(new FileWriter(path, true)); // true - для дописывания, а не перезатирания
        out.write(str);
        out.newLine();
        out.close();
    }

}
