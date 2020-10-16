package com.kedacom.u2f.u2foper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.kedacom.u2f.common.ResponseStateInfo;
import com.kedacom.u2f.consts.U2fConsts;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author : wx
 * @version : 1
 * @date : 2020/10/16 14:15
 */
@Slf4j
public class BaseController {

    protected final JsonNodeFactory jsonFactory = JsonNodeFactory.instance;

    protected final ObjectMapper jsonMapper = new ObjectMapper()
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
            .registerModule(new Jdk8Module());

    protected ResponseStateInfo jsonFail(ResponseStateInfo lsi) {
        lsi.setResponseState(U2fConsts.ResponseState.SERVER_ERROR.getStateId());
        return lsi;
    }

    protected ResponseStateInfo messagesJson(int state, String message) {
        return messagesJson(state, Arrays.asList(message));
    }

    protected ResponseStateInfo messagesJson(int state, List<String> messages) {
        ResponseStateInfo lsi = new ResponseStateInfo();
        log.debug("Encoding messages as JSON: {}", messages);
        lsi.setResponseState(state);
        try {
            lsi.setResponseData(writeJson(
                    jsonFactory.objectNode()
                            .set("messages", jsonFactory.arrayNode()
                                    .addAll(messages.stream().map(jsonFactory::textNode).collect(Collectors.toList()))
                            )
            ));
            return lsi;
        } catch (JsonProcessingException e) {
            log.error("Failed to encode messages as JSON: {}", messages, e);
            return jsonFail(lsi);
        }
    }

    protected String writeJson(Object o) throws JsonProcessingException {
        return jsonMapper.writeValueAsString(o);
    }
}
