//package com.boonya.game.langchain4j.openai;
//
//import dev.langchain4j.model.openai.OpenAiChatModel;
//
//public class FreeDemo {
//    public static void main(String[] args) {
//        OpenAiChatModel model = OpenAiChatModel.builder()
//                .baseUrl("http://langchain4j.dev/demo/openai/v1")
//                .apiKey("openai")
//                .modelName("gpt-4o-mini")
//                .build();
//        // TODO 注意这里没有实现多历史对话记忆能力
//        String result = model.chat("生活的意义是什么?");
//        System.out.println(result);
//    }
//}
