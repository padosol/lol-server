package com.example.lolserver.riot;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Getter
@Setter
@Builder
public class MatchParameters {

        private Long startTime;
        private Long endTime;
        private Integer queue;
        private String type;

        @Builder.Default
        private Integer start = 0;

        @Builder.Default
        private Integer count = 20;


        public MultiValueMap<String, String> getParams() {

                MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

                if(this.startTime != null) {
                        params.add("startTime", this.startTime.toString());
                }

                if(this.endTime != null) {
                        params.add("endTime", this.endTime.toString());
                }

                if(this.queue != null) {
                        params.add("queue", this.queue.toString());
                }

                if(this.type != null) {
                        params.add("type", this.type);
                }

                if(this.start != null) {
                        params.add("start", this.start.toString());
                }

                if(this.count != null) {
                        params.add("count", this.count.toString());
                }


                return params;
        }

}
