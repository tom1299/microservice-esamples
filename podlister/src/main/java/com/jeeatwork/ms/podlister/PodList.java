package com.jeeatwork.ms.podlister;

import lombok.Data;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
@ToString
public class PodList {

    private String namespace;

    private List<String> names = new ArrayList<>();
}
