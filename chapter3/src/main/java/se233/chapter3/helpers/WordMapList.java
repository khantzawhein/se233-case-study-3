package se233.chapter3.helpers;

import se233.chapter3.model.FileFreq;

import java.util.ArrayList;
import java.util.List;

public class WordMapList extends ArrayList<FileFreq> implements Comparable<WordMapList> {
    private int totalFreq = 0;

    public WordMapList(List<FileFreq> fileFreqs) {
        super(fileFreqs);
    }

    public int getTotalFreq() {
        if (totalFreq == 0) {
            countTotalFreq();
        }
        return totalFreq;
    }

    public void countTotalFreq() {
        for (FileFreq fileFreq : this) {
            this.totalFreq += fileFreq.getFreq();
        }
    }

    @Override
    public int compareTo(WordMapList fileFreqs) {
        return Integer.compare(fileFreqs.getTotalFreq(), this.getTotalFreq());
    }
}
