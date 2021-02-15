package com.jeeatwork.ms.podlister;

import lombok.Data;
import lombok.Setter;

import java.util.List;

@Data
public class PodList {

    private String namespace;

    private List<String> names;
}
