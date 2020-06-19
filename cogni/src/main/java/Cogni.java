import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import javax.mail.*;

public class Cogni {

    /**
     * fetches data from all the countries.
     * @return returns JSONArray of Objects
     * @throws UnirestException
     */
    public JSONArray fetchData() throws UnirestException{
        HttpResponse<String> response = Unirest.get("https://covid-19-data.p.rapidapi.com/help/countries?format=json")
                .header("x-rapidapi-host", "covid-19-data.p.rapidapi.com")
                .header("x-rapidapi-key", "62ca314a2emsh9850f59af7aa7ecp1f2b6cjsn5b5d13da6ebb")
                .asString();

        return new JSONArray(response.getBody());
    }

    /**
     * sorts the whole list of JSONObjects by latitude.
     * @return top 15 countries descending order.
     * @throws UnirestException
     */
    public ArrayList<JSONObject> sortAndGetTop15() throws UnirestException {
        JSONArray jsonArray = fetchData();
        ArrayList<JSONObject> jsonList = new ArrayList<JSONObject>();
        ArrayList<JSONObject> nullJsonList = new ArrayList<JSONObject>();
        ArrayList<JSONObject> top15 = new ArrayList<JSONObject>();

        for(int i = 0; i < jsonArray.length(); i++){
            if(((JSONObject)jsonArray.get(i)).isNull("latitude")){
                nullJsonList.add((JSONObject) jsonArray.get(i));
            }
            else{
                jsonList.add((JSONObject) jsonArray.get(i));
            }
        }

        Collections.sort(jsonList, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject o1, JSONObject o2) {
                try {
                    Double f1 = (Double) o1.get("latitude");
                    Double f2 = (Double) o2.get("latitude");
                    return f2.compareTo(f1);
                }catch(Exception e){
                    System.out.println(e);
                    return -1;
                }
            }
        });
        jsonList.addAll(nullJsonList);

        for(int i = 0; i < 15; i++){
            top15.add(jsonList.get(i));
        }

        return top15;
    }

    /**
     * fetches new data and stores 5 countries from the last 10 days.
     * @return  Array of stored countries.
     * @throws UnirestException
     * @throws InterruptedException
     */
    public JSONArray[] getFiveCountryData() throws UnirestException, InterruptedException {
        JSONArray[] countries = new JSONArray[5];
        String[] arrCountries = new String[]{"lt", "lv", "pl", "ee", "ru"};
        String[] arrDates = {"2020-06-06", "2020-06-07", "2020-06-08", "2020-06-09", "2020-06-10", "2020-06-11", "2020-06-12", "2020-06-13",
                "2020-06-14", "2020-06-15"};
        int loader = 0;

        for(int i = 0; i < arrCountries.length; i++){
            countries[i] = new JSONArray();
            for(int j = 0; j < arrDates.length; j++) {
                HttpResponse<String> response = Unirest.get("https://covid-19-data.p.rapidapi.com/report/country/code?format=json&date-format=YYYY-MM-DD&date=" + arrDates[j] + "&code=" + arrCountries[i])
                        .header("x-rapidapi-host", "covid-19-data.p.rapidapi.com")
                        .header("x-rapidapi-key", "cc8c4a9a56msh1693e4aad0ab4f6p1d4750jsnab21a6516aa0")
                        .asString();
                JSONArray jsonArray = new JSONArray(response.getBody());

                String counCode = arrCountries[i];
                String date = arrDates[j];
                Object confirmed = ((JSONObject)((JSONArray)((JSONObject) jsonArray.get(0)).get("provinces")).get(0)).get("confirmed");
                Object active = ((JSONObject)((JSONArray)((JSONObject) jsonArray.get(0)).get("provinces")).get(0)).get("active");
                Object deaths = ((JSONObject)((JSONArray)((JSONObject) jsonArray.get(0)).get("provinces")).get(0)).get("deaths");
                Object recovered = ((JSONObject)((JSONArray)((JSONObject) jsonArray.get(0)).get("provinces")).get(0)).get("recovered");

                JSONObject data = new JSONObject();
                data.put("code",  counCode);
                data.put("date", date);
                data.put("confirmed", confirmed);
                data.put("active", active);
                data.put("deaths", deaths);
                data.put("recovered", recovered);

                countries[i].put(data);

                System.out.print("Processing: ");
                System.out.print(loader + "%...");
                loader += 2;
                Thread.sleep(1500);
                System.out.print("\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b");

            }
        }

        return countries;
    }

    /**
     * finds countries that had biggest death toll any day.
     * @return Array of JSONObjects of countries with biggest death toll any day.
     * @throws InterruptedException
     * @throws UnirestException
     */
    public JSONObject[] getMaxDeaths() throws InterruptedException, UnirestException {
        JSONArray[] data = getFiveCountryData();
        JSONObject[] maxDeaths = new JSONObject[data.length];

        for(int i = 0; i < data.length; i++){
            maxDeaths[i] = (JSONObject) data[i].get(data[i].length()-1);
        }

        return maxDeaths;
    }

    /**
     * generates XML file of the countries with highest data and sends it via Email.
     * @param emailer Emailer class Object
     * @throws UnirestException
     * @throws InterruptedException
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     * @throws TransformerException
     * @throws MessagingException
     */
    public void createXMLandEmail(Emailer emailer, String recipient) throws UnirestException, InterruptedException, ParserConfigurationException,
            IOException, SAXException, TransformerException, MessagingException {
        JSONObject[] arrOfJson = getMaxDeaths();
        String[] xmlStr = new String[arrOfJson.length];

        for(int i = 0; i < arrOfJson.length; i++){
            xmlStr[i] = "<?xml version=\"1.0\" encoding=\"ISO-8859-15\"?>\n<" + "root>" + XML.toString(arrOfJson[i]) + "</root>";
        }

        for(int i = 0; i < arrOfJson.length ; i++){
            stringToXmlFile(xmlStr[i], arrOfJson[i].getString("code"));
            emailer.sendEmail(recipient, "XML files", getHTMLtext(xmlStr[i]), arrOfJson[i].getString("code") + ".xml");
        }

    }

    /**
     * Creates XML file from a passed JSONObject passed as a string.
     * @param xmlStr    String of the JSONObject.
     * @param country   Country`s code to name a file.
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     * @throws TransformerException
     */
    private void stringToXmlFile(String xmlStr, String country) throws ParserConfigurationException, IOException, SAXException,
            TransformerException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();;
        Document doc = builder.parse(new InputSource(new StringReader(xmlStr)));

        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = tFactory.newTransformer();
        DOMSource domSource = new DOMSource(doc);

        StreamResult result = new StreamResult(new File(country + ".xml"));
        transformer.transform(domSource, result);
    }

    /**
     * return a text with a HTML body
     * @param text Sting in passed in html body
     * @return  return a text with a HTML body
     */
    public String getHTMLtext(String text){
        return "<html>" +
                "<head><title>Hello World</title></head>" +
                "<body><p>" + text + "</p></body>" +
                "</html>";
    }


    public static void main(String args[]) throws UnirestException, InterruptedException, ParserConfigurationException,
            TransformerException, SAXException, IOException, MessagingException {

        Cogni c = new Cogni();
        // log in to your gmail, dont forget to turn off 'Less secure app access' in your gmail account.
        Emailer emailer = new Emailer("myGmail@gmail.com", "myPassword");

        // Task 1: fetch covid data
        //**Uncomment**//
        //System.out.println(c.fetchData());

        // Task 2: Sort countries by Latitude Descending order. Select top 15 Countries;
        //**Uncomment**//
        //System.out.println(c.sortAndGetTop15());

        // Task 3 and 4: Get last 10 days data from selected 5 countries, using endpoint.
        //**Uncomment**//
        //System.out.println(Arrays.toString(c.getFiveCountryData()));

        // Task 5: Select stored entries for every country, one day data, that has worst (biggest) death toll.
        //**Uncomment**//
        //System.out.println(Arrays.toString(c.getMaxDeaths()));

        // Task 6: Convert it to XML. Email in html email body and xml-generated file attached.
        //**Uncomment**//
        //c.createXMLandEmail(emailer, "receiversEmail@gmail.com");


    }
}
