package ctbrec.io;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonReader.Token;
import com.squareup.moshi.JsonWriter;

import ctbrec.Model;
import ctbrec.sites.Site;
import ctbrec.sites.chaturbate.ChaturbateModel;

public class ModelJsonAdapter extends JsonAdapter<Model> {

    private List<Site> sites;

    public ModelJsonAdapter() {
    }

    public ModelJsonAdapter(List<Site> sites) {
        this.sites = sites;
    }

    @Override
    public Model fromJson(JsonReader reader) throws IOException {
        reader.beginObject();
        String name = null;
        String description = null;
        String url = null;
        String type = null;
        int streamUrlIndex = -1;
        while(reader.hasNext()) {
            Token token = reader.peek();
            if(token == Token.NAME) {
                String key = reader.nextName();
                if(key.equals("name")) {
                    name = reader.nextString();
                } else if(key.equals("description")) {
                    description = reader.nextString();
                } else if(key.equals("url")) {
                    url = reader.nextString();
                } else if(key.equals("type")) {
                    type = reader.nextString();
                } else if(key.equals("streamUrlIndex")) {
                    streamUrlIndex = reader.nextInt();
                }
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();

        try {
            Class<?> modelClass = Class.forName(Optional.ofNullable(type).orElse(ChaturbateModel.class.getName()));
            Model model = (Model) modelClass.newInstance();
            model.setName(name);
            model.setDescription(description);
            model.setUrl(url);
            model.setStreamUrlIndex(streamUrlIndex);
            if(sites != null) {
                for (Site site : sites) {
                    if(site.isSiteForModel(model)) {
                        model.setSite(site);
                    }
                }
            }
            return model;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new IOException("Couldn't instantiate mode class [" + type + "]", e);
        }
    }

    @Override
    public void toJson(JsonWriter writer, Model model) throws IOException {
        writer.beginObject();
        writer.name("type").value(model.getClass().getName());
        writeValueIfSet(writer, "name", model.getName());
        writeValueIfSet(writer, "description", model.getDescription());
        writeValueIfSet(writer, "url", model.getUrl());
        writer.name("streamUrlIndex").value(model.getStreamUrlIndex());
        writer.endObject();
    }

    private void writeValueIfSet(JsonWriter writer, String name, String value) throws IOException {
        if(value != null) {
            writer.name(name).value(value);
        }
    }

}
