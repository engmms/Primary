package com.quaap.primary.partsofspeech.plurals;

/**
 * Created by tom on 12/15/16.
 * <p>
 * Copyright (C) 2016   Tom Kliethermes
 * <p>
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.quaap.primary.R;
import com.quaap.primary.base.StdGameActivity;
import com.quaap.primary.base.StdLevel;
import com.quaap.primary.base.SubjectBaseActivity;
import com.quaap.primary.base.component.InputMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PluralActivity extends StdGameActivity
        implements SubjectBaseActivity.AnswerGivenListener<String>,
        SubjectBaseActivity.AnswerTypedListener {


    private final int numanswers = 4;
    int hintStart = 4;
    int hintPos = 0;
    private List<String> words;
    private String word;
    private String answer;
    private String[] unpluralMap;
    private Map<String, String> pluralsMap;
    private String[] wordScores;

    public PluralActivity() {
        super(R.layout.std_plural_prob);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        words = Arrays.asList(getResources().getStringArray(R.array.common_nouns));

        unpluralMap = getResources().getStringArray(R.array.unplural);

        wordScores = getResources().getStringArray(R.array.plural_word_scores);

        pluralsMap = arrayPairsToMap(getResources().getStringArray(R.array.plurals));


    }

    private Map<String, String> arrayPairsToMap(String[] array) {

        if (array.length % 2 != 0) {
            throw new IllegalArgumentException("array to map must have even number of elements");
        }
        Map<String, String> map = new TreeMap<>();
        for (int j = 0; j < array.length; j += 2) {
            map.put(array[j], array[j + 1]);
        }
        return map;
    }

    @Override
    protected void onPause() {


        saveLevelValue("word", word);
        super.onPause();
    }

    @Override
    protected void onResume() {

        super.onResume();
        if (isLandscape() && ((StdLevel) getLevel()).getInputMode() == InputMode.Input) {
            LinearLayout problem_area = (LinearLayout) findViewById(R.id.problem_area);
            problem_area.setOrientation(LinearLayout.HORIZONTAL);
        }

    }

    @Override
    protected void onShowLevel() {
        super.onShowLevel();

        if (((PluralLevel) getLevel()).getInputMode() == InputMode.Buttons) {
            setFasttimes(800, 1600, 3000);
        } else {
            setFasttimes(1500, 2200, 5000);
        }

    }

    @Override
    protected void showProbImpl() {

        PluralLevel level = (PluralLevel) getLevel();
        word = getSavedLevelValue("word", (String) null);
        if (word == null) {
            int tries = 0;
            do {
                int score;
                do {
                    word = words.get(getRand(words.size() - 1));
                    score = scoreWord(word);
                    //System.out.println(word + " " + score + " " + pluralsMap.containsKey(word));
                }
                while (!pluralsMap.containsKey(word) || (getRand(10) > 2 && score < 2) || (getRand(10) > 3 && score < 3)); //try to get tricky words

            }
            while (tries++ < 100 && (word.length() < level.getMinWordLength() || word.length() > level.getMaxWordLength() || seenProblem(word)));
        } else {
            deleteSavedLevelValue("word");
        }

        answer = pluralsMap.get(word);
        Log.d("plural", word + " -> " + answer);

        TextView plural = (TextView) findViewById(R.id.txtplural);
        plural.setText(capitalize(word));


        final TextView hint = (TextView) findViewById(R.id.plurHint);
        hint.setText("");

        if (level.getInputMode() == InputMode.Buttons) {
            List<String> answers = getAnswerChoices(answer);

            makeChoiceButtons(getAnswerArea(), answers, this);

        } else if (level.getInputMode() == InputMode.Input) {

            makeInputBox(getAnswerArea(), getKeysArea(), this, INPUTTYPE_TEXT, 5, 0, word);

            hintPos = answer.length() - hintStart;
            if (hintPos < 1) hintPos = 1;
            startHint(6000, 3000);

        } else {
            throw new IllegalArgumentException("Unknown inputMode! " + level.getInputMode());
        }

    }

    @Override
    public boolean answerTyped(String answer) {
        return answerGiven(answer);
    }

    @Override
    public boolean answerGiven(String answer) {

        int points = 0;
        boolean isright = answer.toLowerCase().trim().equals(this.answer.toLowerCase());
        if (isright) {
            points = (int) (1 + word.length() * (levelnum + 1) * scoreWord(word) * (hintStart + answer.length() - (float) hintPos) / answer.length());
        }
        answerDone(isright, points, word, this.answer, answer.trim());

        return isright;
    }

    protected List<String> getAnswerChoices(String realanswer) {
        List<String> answers = new ArrayList<>();
        answers.add(realanswer);
        int maxtries = unpluralMap.length;
        int tries;
        do {
            String badspell;
            tries = 0;
            do {
                badspell = unplural(word);
            } while (tries++ < maxtries && answers.contains(badspell));
            if (tries < maxtries) {
                answers.add(badspell);
            }

        } while (answers.size() < numanswers && tries < maxtries);

        Collections.shuffle(answers);
        return answers;
    }

    @Override
    protected void performHint() {
        final TextView hint = (TextView) findViewById(R.id.plurHint);
        if (hintPos < answer.length()) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    hint.setText(answer.substring(0, hintPos));
                }
            });

            hintPos++;

        } else {
            cancelHint();
        }
    }

    public String unplural(String word) {

        for (int j = 0; j < 1; j++) {
            int i = ((int) (Math.random() * ((unpluralMap.length - 1) / 2)) * 2);
            word = word.replaceFirst(unpluralMap[i], unpluralMap[i + 1]);
        }
        return word;
    }

    private int scoreWord(String candidate) {

        for (int difflevel = wordScores.length - 1; difflevel >= 0; difflevel--) {
            if (candidate.matches(wordScores[difflevel])) {
                return difflevel + 1;
            }

        }

        return 1;
    }

}
