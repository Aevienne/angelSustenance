package me.angelique.angelSustenance.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class PlayerDietData {

    private final Deque<FoodEntry> recentFoods = new ArrayDeque<>();
    private int dietScore;
    private boolean balancedMealActive;

    public Deque<FoodEntry> getRecentFoods() {
        return recentFoods;
    }

    public List<FoodEntry> getRecentFoodsAsList() {
        return new ArrayList<>(recentFoods);
    }

    public int getDietScore() {
        return dietScore;
    }

    public void setDietScore(int dietScore) {
        this.dietScore = Math.max(0, dietScore);
    }

    public boolean isBalancedMealActive() {
        return balancedMealActive;
    }

    public void setBalancedMealActive(boolean balancedMealActive) {
        this.balancedMealActive = balancedMealActive;
    }
}
