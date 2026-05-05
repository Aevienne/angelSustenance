package me.angelique.angelSustenance.model;

public final class FoodEntry {

    private final String foodId;
    private final FoodCategory category;
    private final long consumedAt;

    public FoodEntry(String foodId, FoodCategory category, long consumedAt) {
        this.foodId = foodId;
        this.category = category;
        this.consumedAt = consumedAt;
    }

    public String getFoodId() {
        return foodId;
    }

    public FoodCategory getCategory() {
        return category;
    }

    public long getConsumedAt() {
        return consumedAt;
    }
}
