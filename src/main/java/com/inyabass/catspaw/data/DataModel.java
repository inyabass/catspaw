package com.inyabass.catspaw.data;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.internal.JsonFormatter;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.slf4j.Logger;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class DataModel {

    public final static String ROOT = "$";
    public final static String REPLACEABLE = "£";

    protected Logger logger = null;
    protected DocumentContext documentContext = null;

    public static void main(String[] args) {
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("datamodels/qa/qa_v3.json");
        DataModel dataModel = new DataModel(inputStream);
    }

    public DataModel() {
    }

    public DataModel(StandardModel standardModel) {
        this.load(standardModel.getJson());
    }

    public DataModel(String json) {
        this.load(json);
    }

    public DataModel(InputStream inputStream) {
        this.load(inputStream);
    }

    public void load(String json) {
        try {
            this.documentContext = JsonPath.parse(json);
        } catch (Throwable t) {
            Assert.fail("Unable to Parse JSON: '" + json + "' "  + t.getMessage() + " " + t.getCause());
        }
    }

    public void load(InputStream inputStream) {
        String json = "";
        try {
            json = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (Throwable t) {
            Assert.fail("Unable to Read Inputstream :" + t.getMessage() + " " + t.getCause());
        }
        this.load(json);
    }

    public String export() {
        Assert.assertNotNull("Cannot Export - No Valid Model available", this.documentContext);
        return this.documentContext.jsonString();
    }

    public String prettyExport() {
        Assert.assertNotNull("Cannot Export - No Valid Model available", this.documentContext);
        return JsonFormatter.prettyPrint(this.export());
    }

    public int getNumberOfTopLevelAttributes() {
        JSONObject jsonObject = new JSONObject(this.documentContext.read("$"));
        return jsonObject.size();
    }

    public String getString(String path) {
        Assert.assertNotNull("No Valid Model available", this.documentContext);
        return this.documentContext.read(path);
    }

    public void setString(String path, String value) {
        Assert.assertNotNull("No Valid Model available", this.documentContext);
        this.documentContext.set(path, value);
    }

    public boolean getBoolean(String path) {
        Assert.assertNotNull("No Valid Model available", this.documentContext);
        return this.documentContext.read(path);
    }

    public void setBoolean(String path, boolean value) {
        Assert.assertNotNull("No Valid Model available", this.documentContext);
        this.documentContext.set(path, value);
    }

    public void delete(String path) {
        Assert.assertNotNull("No Valid Model available", this.documentContext);
        this.documentContext.delete(JsonPath.compile(path));
    }

    public void addString(String basePath, String name, String value) {
        Assert.assertNotNull("No Valid Model available", this.documentContext);
        JsonPath jsonPath = JsonPath.compile(basePath);
        this.documentContext.put(jsonPath, name, value);
    }

    public void addBoolean(String basePath, String name, boolean value) {
        Assert.assertNotNull("No Valid Model available", this.documentContext);
        JsonPath jsonPath = JsonPath.compile(basePath);
        this.documentContext.put(jsonPath, name, value);
    }

    public void addObject(String basePath, String name, Object value) {
        Assert.assertNotNull("No Valid Model available", this.documentContext);
        JsonPath jsonPath = JsonPath.compile(basePath);
        this.documentContext.put(jsonPath, name, value);
    }

    public void addStringArrayObject(String basePath, String name) {
        Assert.assertNotNull("No Valid Model available", this.documentContext);
        JsonPath jsonPath = JsonPath.compile(basePath);
        this.documentContext.put(jsonPath, name, new JSONArray());
    }

    public Object getObject(String path) {
        JsonPath jsonPath = JsonPath.compile(path);
        return this.documentContext.read(jsonPath);
    }

    public void setObject(String path, Object object) {
        JsonPath jsonPath = JsonPath.compile(path);
        this.documentContext.set(jsonPath, object);
    }

    public void renameKey(String path, String newName) {
        Assert.assertNotNull("No Valid Model available", this.documentContext);
        String[] parts = path.split("\\.");
        String name = parts[parts.length - 1];
        String newPath = "";
        if(parts.length==1) {
            newPath = "$";
        } else {
            String[] newParts = Arrays.copyOf(parts, parts.length - 1);
            newPath = String.join(".", Arrays.asList(newParts));
        }
        this.documentContext.renameKey(newPath, name, newName);
    }

    public int getSizeOfArray(String path) {
        String sizePath = path + ".size()";
        int size = this.documentContext.read(sizePath);
        return size;
    }

    public void addElementToStringArray(String path, String value) {
        JsonPath jsonPath = JsonPath.compile(path);
        this.documentContext.add(jsonPath, value);
    }

    public String getElementOfStringArray(String path, int index) {
        String elementPath = path + "[" + index + "]";
        return this.getString(elementPath);
    }

    public List<String> getPropertiesOf(String path) {
        Map<String, Object> fields = this.documentContext.read(path);
        return new ArrayList<String>(fields.keySet());
    }

    protected String replace(String string, int index) {
        return string.replace(REPLACEABLE, String.valueOf(index));
    }

    protected String replace(String string, int[] index) {
        if(index.length==0) {
            return string;
        }
        String result = string;
        for(int i = 0;i<index.length;i++) {
            if(result.contains(REPLACEABLE)) {
                result = result.replaceFirst(REPLACEABLE, String.valueOf(index[i]));
            }
        }
        return result;
    }
}
