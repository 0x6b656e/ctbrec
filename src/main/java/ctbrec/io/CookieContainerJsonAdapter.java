package ctbrec.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonReader.Token;
import com.squareup.moshi.JsonWriter;

import ctbrec.io.HttpClient.CookieContainer;
import okhttp3.Cookie;

public class CookieContainerJsonAdapter extends JsonAdapter<CookieContainer> {

    private CookieJsonAdapter cookieAdapter = new CookieJsonAdapter();

    @Override
    public CookieContainer fromJson(JsonReader reader) throws IOException {
        CookieContainer cookies = new CookieContainer();
        reader.beginArray();
        while(reader.hasNext()) {
            reader.beginObject();
            reader.nextName(); // "domain"
            String domain = reader.nextString();
            reader.nextName(); // "cookies"
            reader.beginArray();
            List<Cookie> cookieList = new ArrayList<>();
            while(reader.hasNext()) {
                Token token = reader.peek();
                if(token == Token.END_ARRAY) {
                    break;
                }
                Cookie cookie = cookieAdapter.fromJson(reader);
                cookieList.add(cookie);
            }
            reader.endArray();
            reader.endObject();
            cookies.put(domain, cookieList);
        }
        reader.endArray();
        return cookies;
    }

    @Override
    public void toJson(JsonWriter writer, CookieContainer cookieContainer) throws IOException {
        writer.beginArray();
        for (Entry<String, List<Cookie>> entry : cookieContainer.entrySet()) {
            writer.beginObject();
            writer.name("domain").value(entry.getKey());
            writer.name("cookies");
            writer.beginArray();
            for (Cookie cookie : entry.getValue()) {
                cookieAdapter.toJson(writer, cookie);
            }
            writer.endArray();
            writer.endObject();
        }
        writer.endArray();
    }

}
