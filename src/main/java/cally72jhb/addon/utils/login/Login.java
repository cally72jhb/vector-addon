package cally72jhb.addon.utils.login;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Login {
    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println(getUserIDs());
    }

    public static List<String> getUserIDs() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://raw.githubusercontent.com/cally72jhb/vector-login/master/users"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        List<String> list = Arrays.asList(response.body().replace(" ", "").split("\n"));

        for (int i = 0; i < list.size(); i++) {
            list.set(i, list.get(i).split(":")[0]);
        }

        System.out.println(list);
        System.out.println(mc.getSession().getProfile().getId().toString());

        return list;
    }

    public static List<String> getUserNames() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://raw.githubusercontent.com/cally72jhb/vector-login/master/users"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        List<String> list = Arrays.asList(response.body().replace(" ", "").split("\n"));

        for (int i = 0; i < list.size(); i++) {
            list.set(i, list.get(i).split(":")[1]);
        }

        return list;
    }
}
