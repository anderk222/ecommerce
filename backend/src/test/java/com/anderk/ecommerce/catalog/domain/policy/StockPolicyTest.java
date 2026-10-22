package com.anderk.ecommerce.catalog.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.anderk.ecommerce.catalog.domain.model.StockState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StockPolicyTest {

    private final StockPolicy policy = new StockPolicy();

    @Test
    @DisplayName("a null stock means the stock is not tracked")
    void nullStockMeansTheStockIsNotTracked() {
        assertThat(this.policy.stateOf(null, 5)).isEqualTo(StockState.NOT_TRACKED);
    }

    @Test
    @DisplayName("zero units means the item is out of stock")
    void zeroUnitsMeansTheItemIsOutOfStock() {
        assertThat(this.policy.stateOf(0, 5)).isEqualTo(StockState.OUT_OF_STOCK);
    }

    @Test
    @DisplayName("at or below the threshold means low stock")
    void atOrBelowTheThresholdMeansLowStock() {
        assertThat(this.policy.stateOf(5, 5)).isEqualTo(StockState.LOW_STOCK);
        assertThat(this.policy.stateOf(2, 5)).isEqualTo(StockState.LOW_STOCK);
    }

    @Test
    @DisplayName("above the threshold means the item is in stock")
    void aboveTheThresholdMeansTheItemIsInStock() {
        assertThat(this.policy.stateOf(6, 5)).isEqualTo(StockState.IN_STOCK);
    }

    @Test
    @DisplayName("an item that does not track stock can always be sold")
    void anItemThatDoesNotTrackStockCanAlwaysBeSold() {
        assertThat(this.policy.canDecrease(null, 100)).isTrue();
    }

    @Test
    @DisplayName("a tracked item cannot be sold beyond its stock")
    void aTrackedItemCannotBeSoldBeyondItsStock() {
        assertThat(this.policy.canDecrease(3, 3)).isTrue();
        assertThat(this.policy.canDecrease(3, 4)).isFalse();
    }
}
