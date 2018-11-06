package ctbrec.io;

import java.io.IOException;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;

import okhttp3.Cookie;
import okhttp3.Cookie.Builder;

public class CookieJsonAdapter extends JsonAdapter<Cookie> {

    @Override
    public Cookie fromJson(JsonReader reader) throws IOException {
        reader.beginObject();
        Builder builder = new Cookie.Builder();
        // domain
        reader.nextName();
        String domain = reader.nextString();
        builder.domain(domain);

        // expiresAt
        reader.nextName();
        builder.expiresAt(reader.nextLong());

        // host only
        reader.nextName();
        if(reader.nextBoolean()) {
            builder.hostOnlyDomain(domain);
        }

        // http only
        reader.nextName();
        if(reader.nextBoolean()) {
            builder.httpOnly();
        }

        // name
        reader.nextName();
        builder.name(reader.nextString());

        // path
        reader.nextName();
        builder.path(reader.nextString());

        // persistent
        reader.nextName();
        if(reader.nextBoolean()) {
            // noop
        }

        // secure
        reader.nextName();
        if(reader.nextBoolean()) {
            builder.secure();
        }

        // value
        reader.nextName();
        builder.value(reader.nextString());

        reader.endObject();
        return builder.build();
    }

    @Override
    public void toJson(JsonWriter writer, Cookie cookie) throws IOException {
        writer.beginObject();
        writer.name("domain").value(cookie.domain());
        writer.name("expiresAt").value(cookie.expiresAt());
        writer.name("hostOnly").value(cookie.hostOnly());
        writer.name("httpOnly").value(cookie.httpOnly());
        writer.name("name").value(cookie.name());
        writer.name("path").value(cookie.path());
        writer.name("persistent").value(cookie.persistent());
        writer.name("secure").value(cookie.secure());
        writer.name("value").value(cookie.value());
        writer.endObject();
    }
}
