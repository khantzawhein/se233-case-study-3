package se233.chapter3.controller;

import se233.chapter3.helpers.WordMapList;
import se233.chapter3.model.FileFreq;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class WordMapMergeTask implements Callable<LinkedHashMap<String, ArrayList<FileFreq>>> {
    private Map<String, FileFreq>[] wordmap;

    public WordMapMergeTask(Map<String, FileFreq>[] wordmap) {
        this.wordmap = wordmap;
    }

    @Override
    public LinkedHashMap<String, ArrayList<FileFreq>> call() {
        List<Map<String, FileFreq>> wordMapList = new ArrayList<>(Arrays.asList(wordmap));

        return wordMapList.stream()
                .flatMap(m -> m.entrySet().stream())
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collector.of(
                                () -> new WordMapList(new ArrayList<>()),
                                (list, item) -> {
                                    list.add(item.getValue());
                                },
                                (current_list, new_items) -> {
                                    current_list.addAll(new_items);
                                    return current_list;
                                }
                        )
                ))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(item -> {
                    ArrayList<Integer> freqs = new ArrayList<>();
                    item.getValue().forEach(fileFreq -> freqs.add(fileFreq.getFreq()));
                    String joinedFreqs = freqs.stream()
                            .sorted(Comparator.reverseOrder())
                            .map(Object::toString)
                            .collect(Collectors.joining(","));
                    return item.getKey() + " ("  + joinedFreqs + ")";
                }, Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));
    }
}
