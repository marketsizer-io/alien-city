import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.catalyst.advanced.CatalystAdvancedIOHandler;
import com.zc.common.ZCProject;
import com.zc.component.object.ZCObject;
import com.zc.component.object.ZCRowObject;
import com.zc.component.zcql.ZCQL;

public class AlienCityAIO implements CatalystAdvancedIOHandler {
    private static final Logger LOGGER = Logger.getLogger(AlienCityAIO.class.getName());

    private static final String TABLENAME = "AlienCity";
    private static final String COLUMNNAME = "CityName";
    private JSONObject responseData = new JSONObject();
    private static final String GET = "GET";
    private static final String POST = "POST";

    @Override
    @SuppressWarnings("unchecked")
    public void runner(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            ZCProject.initProject();

            // Fetches the endpoint and method to which the call was made
            String url = request.getRequestURI();
            String method = request.getMethod();

            // The GET API that checks the table for an alien encounter in that city
            if (url.equals("/alien") && method.equals(GET)) {
                String cityName = request.getParameter("city_name");

                // Queries the Catalyst Data Store table and checks whether a row is present for the given city
                int length = getAlienCountFromCatalystDataStore(cityName);
                if (length > 0) {
                    responseData.put("message", "Uh oh! Looks like there are aliens in this city!");
                    responseData.put("signal", "positive");
                } else {
                    responseData.put("message", "Hurray! No alien encounters in this city yet!");
                    responseData.put("signal", "negative");
                }
            }

            // The POST API that reports the alien encounter for a particular city
            else if (url.equals("/alien") && method.equals(POST)) {
                // Gets the request body and parses it
                ServletInputStream requestBody = request.getInputStream();
                JSONParser jsonParser = new JSONParser();
                JSONObject jsonObject = (JSONObject) jsonParser.parse(new InputStreamReader(requestBody, "UTF-8"));

                String cityName = (String) jsonObject.get("city_name");

                // Queries the Catalyst Data Store table and checks whether a row is present for the given city
                int length = getAlienCountFromCatalystDataStore(cityName);
                if (length > 0) {
                    responseData.put("message", "Looks like you are not the first person to encounter aliens in this city! Someone has already reported an alien encounter here!");
                } else {
                    // If the row is not present, then a new row is inserted
                    ZCRowObject row = ZCRowObject.getInstance();
                    row.set("CityName", cityName);
                    ZCObject.getInstance().getTableInstance(TABLENAME).insertRow(row);
                    responseData.put("message", "Thanks for reporting!");
                }
            } else {
                // The actions are logged. You can check the logs from Catalyst Logs.
                LOGGER.log(Level.SEVERE, "Error. Invalid Request");
                responseData.put("error", "Request Endpoint not found");
                response.setStatus(404);
            }

            // Sends the response back to the Client
            response.setContentType("application/json");
            response.getWriter().write(responseData.toString());
            response.setStatus(200);
        } catch (Exception e) {
            // The actions are logged. You can check the logs from Catalyst Logs.
            LOGGER.log(Level.SEVERE, "Exception in AlienCityAIO", e);
            responseData.put("error", "Internal server error occurred. Please try again in some time.");
            response.getWriter().write(responseData.toString());
            response.setStatus(500);
        }
    }

    /**
     * Checks whether an alien encounter is already reported for the given city by querying the Data Store table
     *
     * @param cityName The name of the city to check
     * @return The number of rows found
     * @throws Exception If an error occurs while querying the Data Store
     */
    private int getAlienCountFromCatalystDataStore(String cityName) throws Exception {
        String query = "select * from " + TABLENAME + " where " + COLUMNNAME + " = '" + cityName + "'";

        // Gets the ZCQL instance and executes query using the query string
        ArrayList<ZCRowObject> rowList = ZCQL.getInstance().executeQuery(query);
        return rowList.size();
    }
}
