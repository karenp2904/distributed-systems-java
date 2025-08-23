package soapserver;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.ByteArrayInputStream;
import java.util.List;

public class SOAPProcessor {
    private final SongService songService;

    public SOAPProcessor(SongService songService) {
        this.songService = songService;
    }

    public String processSOAPRequest(String soapRequest) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(soapRequest.getBytes()));

            String[] possibleMethods = {"searchByTitle", "searchByGenre", "searchByAuthor", "searchByMultipleCriteria"};

            NodeList methodNodes = doc.getElementsByTagName("*");
            for (int i = 0; i < methodNodes.getLength(); i++) {
                Element element = (Element) methodNodes.item(i);
                String localName = element.getLocalName();

                for (String method : possibleMethods) {
                    if (method.equals(localName)) {
                        switch (method) {
                            case "searchByTitle":
                                return createSOAPResponse(songService.searchByTitle(getParameterValue(element, "arg0")), "searchByTitleResponse");

                            case "searchByGenre":
                                return createSOAPResponse(songService.searchByGenre(getParameterValue(element, "arg0")), "searchByGenreResponse");

                            case "searchByAuthor":
                                return createSOAPResponse(songService.searchByAuthor(getParameterValue(element, "arg0")), "searchByAuthorResponse");

                            case "searchByMultipleCriteria":
                                return createSOAPResponse(
                                        songService.searchByMultipleCriteria(
                                                getParameterValue(element, "arg0"),
                                                getParameterValue(element, "arg1"),
                                                getParameterValue(element, "arg2")),
                                        "searchByMultipleCriteriaResponse");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return createSOAPFault("Invalid request");
    }

    private String getParameterValue(Element methodElement, String paramName) {
        NodeList params = methodElement.getElementsByTagName(paramName);
        if (params.getLength() > 0) {
            return params.item(0).getTextContent();
        }
        return "";
    }

    private String createSOAPResponse(List<Song> songs, String methodName) {
        StringBuilder response = new StringBuilder();
        response.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        response.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">");
        response.append("<soap:Body>");
        response.append("<ns2:").append(methodName).append(" xmlns:ns2=\"http://service.musiclibrary.com/\">");

        for (Song song : songs) {
            response.append("<return>");
            response.append("<title>").append(SOAPUtils.escapeXml(song.getTitle())).append("</title>");
            response.append("<genre>").append(SOAPUtils.escapeXml(song.getGenre())).append("</genre>");
            response.append("<author>").append(SOAPUtils.escapeXml(song.getAuthor())).append("</author>");
            response.append("<language>").append(SOAPUtils.escapeXml(song.getLanguage())).append("</language>");
            response.append("<year>").append(song.getYear()).append("</year>");
            response.append("</return>");
        }

        response.append("</ns2:").append(methodName).append(">");
        response.append("</soap:Body>");
        response.append("</soap:Envelope>");
        return response.toString();
    }

    private String createSOAPFault(String message) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                "<soap:Body>" +
                "<soap:Fault>" +
                "<faultcode>Server</faultcode>" +
                "<faultstring>" + SOAPUtils.escapeXml(message) + "</faultstring>" +
                "</soap:Fault>" +
                "</soap:Body>" +
                "</soap:Envelope>";
    }
}
