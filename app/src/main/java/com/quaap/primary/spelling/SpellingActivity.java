package com.quaap.primary.spelling;

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
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.quaap.primary.Primary;
import com.quaap.primary.R;
import com.quaap.primary.base.StdGameActivity;
import com.quaap.primary.base.SubjectBaseActivity;
import com.quaap.primary.base.component.InputMode;
import com.quaap.primary.base.component.TextToVoice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SpellingActivity extends StdGameActivity
        implements TextToVoice.VoiceReadyListener,
        SubjectBaseActivity.AnswerGivenListener<String>,
        SubjectBaseActivity.AnswerTypedListener {


    final protected Handler handler = new Handler();
    private final int numanswers = 4;
    TextToVoice v;
    private List<String> words;
//    private volatile boolean showHint = false;
    private String word;
    private String[] unspellMap;
    //    private Timer timer;
//    private TimerTask hinttask;
    private volatile int hintPos = 0;
    private String wordStart;


    public SpellingActivity() {
        super(R.layout.std_spelling_prob);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        unspellMap = getResources().getStringArray(R.array.unspell);

        if (unspellMap.length % 2 != 0) {
            throw new IllegalArgumentException("unspell array must have even number of arguments");
        }

        super.onCreate(savedInstanceState);


        Button b = (Button) findViewById(R.id.btn_repeat);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                v.speak(word);
            }
        });
    }

    @Override
    protected void onPause() {
        v.stop();
//        cancelHint();
//
//        if (timer!=null) {
//            timer.cancel();
//            timer.purge();
//            timer=null;
//        }

        saveLevelValue("word", word);
        super.onPause();
    }

    @Override
    protected void onResume() {
        //timer = new Timer();

        setReadyForProblem(false);
        findViewById(R.id.spelling_problem_area).setVisibility(View.INVISIBLE);
        findViewById(R.id.spell_loading).setVisibility(View.VISIBLE);

        super.onResume();


        if (isLandscape()) {
            LinearLayout spelling_problem_area = (LinearLayout) findViewById(R.id.spelling_problem_area);
            spelling_problem_area.setOrientation(LinearLayout.HORIZONTAL);
        }

        v = ((Primary) getApplicationContext()).getTextToVoice();
        v.setVoiceReadyListener(this);
        if (v.isReady()) {
            setWeReady();
        }


    }

    @Override
    protected void onShowLevel() {
        super.onShowLevel();
        words = Arrays.asList(getResources().getStringArray(((SpellingLevel) getLevel()).getmWordlistId()));

        if (((SpellingLevel) getLevel()).getInputMode() == InputMode.Buttons) {
            setFasttimes(800, 1600, 3000);
            //  showHint = false;
        } else {
            setFasttimes(1500, 2200, 5000);
            // showHint = true;
        }

    }

    @Override
    protected void showProbImpl() {

        word = getSavedLevelValue("word", (String) null);
        if (word == null) {
            int tries = 0;
            do {
                word = words.get(getRand(words.size() - 1));
            } while (tries++ < 50 && seenProblem(word));
        } else {
            deleteSavedLevelValue("word");
        }

        Log.d("spell", word);

        SpellingLevel level = (SpellingLevel) getLevel();

        if (level.getInputMode() == InputMode.Buttons) {
            List<String> answers = getAnswerChoices(word);

            makeChoiceButtons(getAnswerArea(), answers, this);

        } else if (level.getInputMode() == InputMode.Input) {

            makeInputBox(getAnswerArea(), getKeysArea(), this, INPUTTYPE_TEXT, 5, 0);
        } else {
            throw new IllegalArgumentException("Unknown inputMode! " + level.getInputMode());
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                v.speak(word);
            }
        }, 500);
        ((TextView) findViewById(R.id.spell_hint)).setText("");


    }

    @Override
    public boolean answerTyped(String answer) {
        return answerGiven(answer);
    }

    @Override
    public boolean answerGiven(String answer) {

        int points = 0;
        boolean isright = answer.toLowerCase().trim().equals(word.toLowerCase());
        if (isright) {
            points = (int) (1 + word.length() * (levelnum + 1) * (1 + word.length() - (float) hintPos) / (word.length()));
        }
        answerDone(isright, points, word, word, answer.trim());

        if (isright) {
            cancelHint();
        }
        return isright;
    }

    @Override
    public void onVoiceReady(TextToVoice ttv) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                setWeReady();
            }
        });
        Log.d("sp1", "onVoiceReady called");
    }

    protected void setWeReady() {
        findViewById(R.id.spelling_problem_area).setVisibility(View.VISIBLE);
        findViewById(R.id.spell_loading).setVisibility(View.GONE);
        setReadyForProblem(true);
    }

    @Override
    public void onSpeakComplete(TextToVoice ttv) {
        if (wordStart == null || !wordStart.equals(word)) {
            startTimer();
            hintPos = 0;
            startHint(5000, 3000);
            wordStart = word;
        }
    }

    @Override
    protected void performHint() {
        final TextView hint = (TextView) findViewById(R.id.spell_hint);
        if (hintPos < word.length()) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    hint.setText(word.substring(0, hintPos));
                }
            });

            hintPos++;
            if (hintPos == 1 || (word.length() > 3 && hintPos == word.length())) {
                v.speak(word);
            }

        } else {
            cancelHint();
        }
    }

    @Override
    public void onError(TextToVoice ttv) {

    }

    protected List<String> getAnswerChoices(String realanswer) {
        List<String> answers = new ArrayList<>();
        int maxtries = unspellMap.length;
        int tries;
        do {
            String badspell;
            tries = 0;
            do {
                badspell = unspell(word);
            } while (tries++ < maxtries && answers.contains(badspell));
            if (tries < maxtries) {
                answers.add(badspell);
            }

        } while (answers.size() < numanswers && tries < maxtries);

        Collections.shuffle(answers);
        return answers;
    }


    public String unspell(String word) {

        for (int j = 0; j < 1; j++) {
            int i = ((int) (Math.random() * ((unspellMap.length - 1) / 2)) * 2);

            String find = unspellMap[i];
            String replacement = unspellMap[i + 1];

            word = word.replaceFirst(find, replacement);
        }
        return word;
    }


}
