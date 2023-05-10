package main.java.client.connect;

import org.json.JSONArray;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.spec.RSAOtherPrimeInfo;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Random;

public class Connect implements Runnable {
    private int port;
    private Socket socket;
    private BufferedWriter ouput;
    private BufferedReader input;
    private ObjectOutputStream outputOb;

    public Connect(Socket socket) throws Exception{
        this.socket = socket;
        System.out.println("Accept Client: " + socket.toString());
        outputOb = new ObjectOutputStream(socket.getOutputStream());
        input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    // gửi dữ liệu qua client
    private void send(LinkedHashMap<String, String> data) {
        try {
            outputOb.writeObject(data);
//            ouput = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
//            ouput.write(data + "\n");
//            ouput.flush();
//            hehe.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // nhận từ client
    private String receive() {
        try {
            String data = input.readLine();
            return data;
        } catch (Exception e) {
            return "";
        }
    }

    // đóng cái stream
    private void closeAll() throws IOException {
        outputOb.close();
        ouput.close();
        input.close();
        socket.close();
    }

    // hàm xử lính dữ liệu chính
    private LinkedHashMap<String, String> processData(String data) {
        try {
            LinkedHashMap<String, String> googleResult = getResponseFromGoogle(data);

            String title = googleResult.get("title");
            String subTitle = googleResult.get("subTitle");

            if (subTitle.contains("Bài hát")) {
                String linkVideo = googleResult.get("linkVideo");
                mySleep();
                LinkedHashMap<String, String> songInfo = getLyricFromGG(googleResult);
                mySleep();

                // tim kiem loi bai hat
                if (songInfo == null) {
//                    System.out.println("tim tu bhh" + title + " " + subTitle);
                    mySleep();
                    String link = getLinkLyricFromBHH(  googleResult);
                    songInfo = getLyricFromBHH(link, googleResult);
                }

                // tìm kiếm nhạc sỹ
                String composer = getComposerFormGG(googleResult);
                if (composer == null) {
                    mySleep();
                    String link = getLinkLyricFromBHH(  googleResult);
                    System.out.println(link);
                    mySleep();
                    composer = getComposerFromBHH(link, googleResult);
                    songInfo.putIfAbsent("songComposer", composer);
                }
                if (composer == null) {
                    songInfo.putIfAbsent("songComposer", null);
                } else {
                    songInfo.putIfAbsent("songComposer", composer);
                }
                songInfo.putIfAbsent("linkVideo", linkVideo);
                return songInfo;
            } else {
//                System.out.println("Ca so");
                System.out.println(title);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    // dùng gg search để tìm tên bài hát và ca sĩ và link bài hát
    private LinkedHashMap<String, String> getResponseFromGoogle(String data) {
        try {

//            System.out.println("data ham tik gg" + data);
            String ggSearchUrl = "https://www.google.com/search?q=";

            String fullUrl = ggSearchUrl + data;

            Document document = Jsoup.connect(fullUrl)
                    .followRedirects(false)
                    .method(Connection.Method.GET)
                    .execute().parse();

            // tach html tu goole search
            Element rnct = document.getElementById("rcnt");
            Element yKMVIe = rnct.getElementsByClass("yKMVIe").get(0);

            LinkedHashMap<String, String> songInfo = new LinkedHashMap<>();

            String title = yKMVIe.text();
//            System.out.println("title " + title);

            title = title.split("\\(")[0].strip();
            if (title.contains("|")) {
                String[] sub = title.split("\\|");
                title = sub[1].strip();
            }
            songInfo.putIfAbsent("title", title);

            Elements wx62f_pzpZlf_x7XAkb = rnct.getElementsByClass("wx62f PZPZlf x7XAkb");
            String[] splitString = wx62f_pzpZlf_x7XAkb.get(0).text().split("\\s");
            String subTitle = wx62f_pzpZlf_x7XAkb.get(0).text();

            String singerName = "";
            for (int i = 3; i < splitString.length; i++) {
                singerName += splitString[i] + " ";
            }
            songInfo.putIfAbsent("subTitle", subTitle);

            try {
                Element H1u2de = rnct.getElementsByClass("H1u2de").first();
                Element tagA = H1u2de.getElementsByTag("a").first();
                songInfo.putIfAbsent("linkVideo", tagA.attr("href"));
            } catch (Exception e ) {
                songInfo.putIfAbsent("linkVideo", null);
            }


            return songInfo;
        } catch (Exception e) {
            System.out.println("Can't connect to Google!!!");
            LinkedHashMap<String, String> tmp = new LinkedHashMap<>();
            tmp.putIfAbsent("title", null);
            tmp.putIfAbsent("subTitle", null);
            return tmp;
        }
    }

    // lấy lời bài hát từ gg search
    private LinkedHashMap<String, String> getLyricFromGG(LinkedHashMap<String, String> songInfo) {
        try {
            LinkedHashMap<String, String> returnHashMap = new LinkedHashMap<>();

            String songName = songInfo.get("title");
            String singerName = songInfo.get("subTitle");

            returnHashMap.putIfAbsent("songName", songName);
            returnHashMap.putIfAbsent("singerName", singerName);

            String ggSearchUrl = "https://www.google.com/search?q=";
            String qLBH = "Lyric ";

//            String fullUrl = ggSearchUrl + qLBH + " " + songName + " của " + singerName;
            String fullUrl = ggSearchUrl + qLBH + " " + songName;

            Document doc = Jsoup.connect(fullUrl)
                    .followRedirects(false)
                    .execute().parse();

            Element search = doc.getElementById("search");
            Element WbKHeb = search.getElementsByAttributeValue("jsname", "WbKHeb").get(0);
            Elements ujudUb = WbKHeb.getElementsByClass("ujudUb");

            String lyric = "";

            for (Element spans: ujudUb) {
                for (Element span: spans.getElementsByTag("span")) {
//                System.out.println(span.text());
                    lyric += span.text() + "\n";
                }
            }

            returnHashMap.putIfAbsent("songLyric", lyric);

            return returnHashMap;
        } catch (Exception e) {
            return null;
        }
    }

    // lấy thông tin tác giả của bài hát thông qua gg search
    private String getComposerFormGG(LinkedHashMap<String, String> songInfo) {
        try {
            String domain = "https://www.google.com/search?q=";
            String songName = songInfo.get("title");
            String[] subTitle = songInfo.get("subTitle").split(" ");

            String singerName = "";
            for (int i = 3; i < subTitle.length; i++) {
                singerName += subTitle[i] + " ";
            }

            String question = "who is the composer of ";

            String fullUrl = domain + question + songName + " " + singerName;

            Document doc = Jsoup.connect(fullUrl)
                    .followRedirects(false)
                    .execute().parse();

            Elements composers = doc.getElementsByClass("bVj5Zb FozYP");

            return composers.first().text();
        } catch (Exception e) {
            return null;
        }

    }


    // lấy thông tin tác giả của bài hát thông qua web baihathay.net
    private String getComposerFromBHH(String linkLyric, LinkedHashMap<String, String> songInfo) {
        try {

            String fullUrl = "https://baihathay.net" + linkLyric;
            Document doc = Jsoup.connect(fullUrl)
                    .followRedirects(false)
                    .execute().parse();

            Element songComposer = doc.getElementsByClass("artist-title").first();
            return songComposer.text();
        } catch (Exception e) {
            return null;
        }
    }

    // lấy link lời bài hát từ baihathay.net
    private String getLinkLyricFromBHH(LinkedHashMap<String, String> songInfo) {
        try {
            String songName = songInfo.get("title").strip();
            songName = songName.replaceAll("\\?", "")
                    .replaceAll("\\*", "")
                    .replaceAll("\\[", "")
                    .replaceAll("\\]", "")
                    .replaceAll("\\$", "")
                    .replaceAll("\\^", "");

            String singerName = "";
            String[] subString = songInfo.get("subTitle").strip().split(" ");

            for (int i = 3; i < subString.length; i++) {
                singerName += subString[i] + " ";
            }

            String bhhUrl = "https://baihathay.net/music/tim-kiem/";
            String fullUrl = bhhUrl + songName + "/trang-1.html";

            Document doc = Jsoup.connect(fullUrl)
                    .followRedirects(false)
                    .execute().parse();

            String link = "";
            Element pureMenuList = doc.getElementsByClass("pure-menu-list").last();
            for (Element pureMenuItem: pureMenuList.getElementsByClass("pure-menu-item")) {
                String song = pureMenuItem.text();
//                int len = song.split("-").length;
                String singer  = song.split("-")[1].strip();

                if (singer.compareToIgnoreCase(singerName.strip()) == 0) {
                    link = pureMenuItem.getElementsByTag("a").first().attr("href");
                    return link;
                }
                if (singer.contains(singerName.strip())) {
                    link = pureMenuItem.getElementsByTag("a").first().attr("href");
                    return link;
                }
            }

            // doạn tìm kiếm link để có thể tìm kiếm nhạc sỹ bài hát
            for (Element pureMenuItem: pureMenuList.getElementsByClass("pure-menu-item")) {
                String line = pureMenuItem.text();
//                int len = song.split("-").length;
                if(line.split("-").length < 3) {
                    continue;
                }
                String song  = line.split("-")[0].strip().toLowerCase();

                if (song.contains(songName.strip().toLowerCase())) {
                    link = pureMenuItem.getElementsByTag("a").first().attr("href");
                    return link;
                }
            }

            pureMenuList = doc.getElementsByClass("pure-menu-list").first();
            link = pureMenuList.getElementsByTag("a").first().attr("href");
            return "/music" + link.split("music")[1];
        } catch (Exception e) {
            System.out.println("Can't get link from BHH");
            return null;
        }
    }

    // lấy lời bài hát từ baihathay.net
    private LinkedHashMap<String, String> getLyricFromBHH(String linkLyric, LinkedHashMap<String, String> songInfo) {
        try {
            String fullUrl = "https://baihathay.net" + linkLyric;
            Document doc = Jsoup.connect(fullUrl)
                    .followRedirects(false)
                    .execute().parse();

            Element songComposer = doc.getElementsByClass("artist-title").first();
            Element tabLyric = doc.getElementsByClass("tab-lyrics").last();
            String lyric = tabLyric.toString().split("\n")[1]
                    .replaceAll("<br>", "\n")
                    .replaceAll("<p>", "")
                    .replaceAll("</p>", "")
                    .strip();


            LinkedHashMap<String, String> returnHashMap = new LinkedHashMap<>();

            returnHashMap.putIfAbsent("singerName", songInfo.get("subTitle"));
            returnHashMap.putIfAbsent("songName", songInfo.get("title"));
            returnHashMap.putIfAbsent("songLyric", lyric);
            returnHashMap.putIfAbsent("songComposer", songComposer.text());

            return returnHashMap;
        } catch (Exception e) {
            return null;
        }
    }

    private String getLinkFormWiki(String name) {
        try {
            String apiLink = "https://vi.wikipedia.org/w/api.php?action=opensearch&search=";
            String url = apiLink + name;

            Document doc = Jsoup.connect(url)
                    .method(Connection.Method.GET)
                    .followRedirects(false)
                    .ignoreContentType(true)
                    .execute().parse();

            JSONArray json = new JSONArray(doc.text());

            JSONArray links = (JSONArray) json.get(3);

            String link = links.get(0).toString();

            String[] subString = link.split("/");
            String singerName = subString[subString.length - 1];
            String decodeSingerName = URLDecoder.decode(singerName, StandardCharsets.UTF_8);
//            System.out.println(decodeSingerName);

            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private void mySleep() {
        Random ran = new Random();
        int ranNum = 1000 + ran.nextInt(400);
        int num = 1;
        for (int i = 0; i < ranNum; i++) {
            num += ranNum * 3 / 2 + ranNum;
        }
    }

    // hàm run từ thread
    @Override
    public void run() {
        try {
            while (true) {
                String data = receive();
                if (data.equals("close")) {
                    break;
                }
                LinkedHashMap<String, String> processedData = processData(data);
                send(processedData);
            }
            System.out.println("Close socket: " + socket.toString());
            closeAll();
        } catch (Exception e) {
            System.out.println("Can't stop!!!");
        }
    }
}