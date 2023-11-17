package com.wojcka.exammanager.components;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ObjectToJson {
    private static ObjectMapper objectMapper = new ObjectMapper();
    public static String toJson(Object object) {
        try {
            String json = objectMapper.writeValueAsString(object);
            return json;
        } catch (Exception ex) {
            return "Error occured while converting to JSON!";
        }
    }
}
