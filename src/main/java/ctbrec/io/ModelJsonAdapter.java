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
        boolean suspended = false;

        Model model = null;
        while(reader.hasNext()) {
            try {
                Token token = reader.peek();
                if(token == Token.NAME) {
                    String key = reader.nextName();
                    if(key.equals("name")) {
                        name = reader.nextString();
                        model.setName(name);
                    } else if(key.equals("description")) {
                        description = reader.nextString();
                        model.setDescription(description);
                    } else if(key.equals("url")) {
                        url = reader.nextString();
                        model.setUrl(url);
                    } else if(key.equals("type")) {
                        type = reader.nextString();
                        Class<?> modelClass = Class.forName(Optional.ofNullable(type).orElse(ChaturbateModel.class.getName()));
                        model = (Model) modelClass.newInstance();
                    } else if(key.equals("streamUrlIndex")) {
                        streamUrlIndex = reader.nextInt();
                        model.setStreamUrlIndex(streamUrlIndex);
                    } else if(key.equals("suspended")) {
                        suspended = reader.nextBoolean();
                        model.setSuspended(suspended);
                    } else if(key.equals("siteSpecific")) {
                        reader.beginObject();
                        model.readSiteSpecificData(reader);
                        reader.endObject();
                    }
                } else {
                    reader.skipValue();
                }
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                throw new IOException("Couldn't instantiate model class [" + type + "]", e);
            }
        }
        reader.endObject();

        if(sites != null) {
            for (Site site : sites) {
                if(site.isSiteForModel(model)) {
                    model.setSite(site);
                }
            }
        }
        return model;
    }

    @Override
    public void toJson(JsonWriter writer, Model model) throws IOException {
        writer.beginObject();
        writer.name("type").value(model.getClass().getName());
        writeValueIfSet(writer, "name", model.getName());
        writeValueIfSet(writer, "description", model.getDescription());
        writeValueIfSet(writer, "url", model.getUrl());
        writer.name("streamUrlIndex").value(model.getStreamUrlIndex());
        writer.name("suspended").value(model.isSuspended());
        writer.name("siteSpecific");
        writer.beginObject();
        model.writeSiteSpecificData(writer);
        writer.endObject();
        writer.endObject();
    }

    private void writeValueIfSet(JsonWriter writer, String name, String value) throws IOException {
        if(value != null) {
            writer.name(name).value(value);
        }
    }

}
