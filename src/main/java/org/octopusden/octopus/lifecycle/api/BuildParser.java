package org.octopusden.octopus.lifecycle.api;

import org.octopusden.octopus.lifecycle.api.entities.Build;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class BuildParser {
    @Autowired
    private GsonBuilder builder;

    private static class MessageStatus {
        public String message;
        public String status;
    }

    private String getStringAfterEqual(String str) {
        return str.substring(str.indexOf("=") + 1, str.lastIndexOf("."));
    }

    @Cacheable(value = "BuildParser-parse", key = "#str")
    public List<Build> parse(String str) {
        List<Build> outList = new ArrayList<>();

        if (str == null) {
            return outList;
        }

        Gson gson = builder.create();

        MessageStatus messageStatus = gson.fromJson(str, MessageStatus.class);

        List<String> stringList = Arrays.stream(messageStatus.message.split(";")).toList();

        for (String string : stringList) {
            List<String> subStrings = Arrays.stream(string.split(" \\| ")).toList();

            if (subStrings.size() > 1) {
                Build build = new Build();

                build.id = subStrings.get(0);

                build.buildDate = getStringAfterEqual(subStrings.get(1));

                if (subStrings.size() == 3) {
                    build.rcDate = null;
                    build.status = subStrings.get(2);
                } else if (subStrings.size() == 4) {
                    build.rcDate = getStringAfterEqual(subStrings.get(2));
                    build.status = subStrings.get(3);
                }

                outList.add(build);
            }
        }

        Collections.reverse(outList);

        return outList;
    }
}
