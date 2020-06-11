package lt.viko.eif.final_project.apiClient;

import lt.viko.eif.final_project.pojos.*;
import org.json.JSONArray;
import org.json.JSONObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Deividas Bagdanskis
 */
public class LaunchLibraryClientImpl implements LaunchLibraryClient {
    private Client client;
    private WebTarget webTarget;
    private DateTimeFormatter formatter;

    public LaunchLibraryClientImpl() {
        client = ClientBuilder.newClient();
        webTarget = client.target("https://launchlibrary.net/1.4");
        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss");
    }
    @Override
    public List<Rocket> getRocketsByName(String name) {
        List<Rocket> rockets = new ArrayList<>();
        Invocation.Builder invBuilder = webTarget.path("rocket/" + name).request(MediaType.APPLICATION_JSON_TYPE);

        String inline = "";

        Response response = invBuilder.get();
        inline = response.readEntity(String.class);
        JSONObject jsonObject = new JSONObject(inline);

        JSONArray array = jsonObject.getJSONArray("rockets");

        for (int i = 0; i < array.length(); i++) {
            Rocket rocket = new Rocket();
            rocket.setName(array.getJSONObject(i).getString("name"));
            rocket.addLink(array.getJSONObject(i).getString("wikiURL"), "wikiURL");
            rockets.add(rocket);
        }
        return rockets;
    }

    @Override
    public List<Launch> getLaunchesByName(String name) {
        List<Launch> launches = new ArrayList<>();
        Invocation.Builder invBuilder = webTarget.path("launch/" + name).request(MediaType.APPLICATION_JSON_TYPE);

        return readLaunchesFromJson(launches, invBuilder);
    }

    @Override
    public List<Launch> getLaunchesByDates(String startDate, String endDate) {
        List<Launch> launches = new ArrayList<>();
        Invocation.Builder invBuilder = webTarget.path("launch/" + startDate + "/" + endDate)
                .request(MediaType.APPLICATION_JSON_TYPE);

        return readLaunchesFromJson(launches, invBuilder);
    }

    @Override
    public List<Mission> getMissionsByName(String name) {
        List<Mission> missions = new ArrayList<>();
        Invocation.Builder invBuilder = webTarget.path("mission/" + name).request(MediaType.APPLICATION_JSON_TYPE);

        Response response = invBuilder.get();

        String inline = "";
        inline = response.readEntity(String.class);

        JSONObject jsonObject = new JSONObject(inline);
        JSONObject launchJSON;
        JSONObject customerJSON;

        JSONArray array = jsonObject.getJSONArray("missions");
        JSONArray agencies;
        JSONArray payloadsJSON;

        for (int i = 0; i < array.length(); i++) {
            Mission mission = new Mission();
            mission.setName(array.getJSONObject(i).getString("name"));
            mission.setDescription(array.getJSONObject(i).getString("description"));
            launchJSON = array.getJSONObject(i).getJSONObject("launch");
            mission.setLaunch(getLaunchesByName(String.valueOf(launchJSON.getInt("id"))).get(0));

            agencies = array.getJSONObject(i).getJSONArray("agencies");
            customerJSON = agencies.getJSONObject(0);

            Customer customer = new Customer();
            customer.setName(customerJSON.getString("name"));
            customer.setCountryCode(customerJSON.getString("countryCode"));
            customer.setWikiURL(customerJSON.getString("wikiURL"));
            mission.setCustomer(customer);

            payloadsJSON = array.getJSONObject(i).getJSONArray("payloads");
            mission.setPayloads(readPayloadsFromJSON(payloadsJSON));

            missions.add(mission);
        }

        return missions;
    }

    private List<Launch> readLaunchesFromJson(List<Launch> launches, Invocation.Builder invBuilder) {
        Response response = invBuilder.get();

        String inline = "";
        inline = response.readEntity(String.class);

        JSONObject jsonObject = new JSONObject(inline);
        JSONObject rocketJSON;
        JSONObject location;
        JSONArray pads;
        JSONObject launchPadJSON;
        JSONObject launchServiceProviderJSON;

        JSONArray array = jsonObject.getJSONArray("launches");

        String windowStart = "";
        String windowEnd = "";

        for (int i = 0; i < array.length(); i++) {
            Launch launch = new Launch();
            launch.setName(array.getJSONObject(i).getString("name"));

            windowStart = array.getJSONObject(i).getString("isostart");
            windowEnd = array.getJSONObject(i).getString("isoend");

            launch.setWindowStart((Instant) formatter.parse(windowStart));
            launch.setWindowEnd((Instant) formatter.parse(windowEnd));

            rocketJSON = array.getJSONObject(i).getJSONObject("rocket");
            Rocket rocket = new Rocket();
            rocket.setName(rocketJSON.getString("name"));
            rocket.addLink(rocketJSON.getString("wikiURL"), "wikiURL");
            launch.setRocket(rocket);

            location = array.getJSONObject(i).getJSONObject("location");
            pads = location.getJSONArray("pads");
            launchPadJSON = pads.getJSONObject(0);

            LaunchPad launchPad = new LaunchPad();
            launchPad.setName(launchPadJSON.getString("name"));
            launchPad.setLongitude(launchPadJSON.getDouble("longitude"));
            launchPad.setLatitude(launchPadJSON.getDouble("latitude"));
            launchPad.setMapsURL(launchPadJSON.getString("mapURL"));
            launchPad.setWikiURL(launchPadJSON.getString("wikiURL"));
            launchPad.setLocationName(location.getString("name"));

            launch.setLaunchPad(launchPad);

            launchServiceProviderJSON = array.getJSONObject(i).getJSONObject("lsp");
            launch.setLaunchServiceProvider(launchServiceProviderJSON.getString("name"));

            launches.add(launch);
        }

        return launches;
    }

    private List<Payload> readPayloadsFromJSON(JSONArray payloadsJSON) {
        List<Payload> payloads = new ArrayList<>();

        for (int i = 0; i < payloadsJSON.length(); i++) {
            Payload payload = new Payload();
            payload.setDescription(payloadsJSON.getJSONObject(i).getString("description"));
            payload.setWeight((int) payloadsJSON.getJSONObject(i).getDouble("weight"));
            payload.setTotalAmount(payloadsJSON.getJSONObject(i).getInt("total"));
            // TODO MissionDAO padaryti jog i db pridetos misijos id butu pridetas prie payloadu mission_id
            payloads.add(payload);
        }

        return  payloads;
    }
}