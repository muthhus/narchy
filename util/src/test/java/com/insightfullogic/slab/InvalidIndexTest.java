package com.insightfullogic.slab;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class InvalidIndexTest {

    @Test
    public void countIsNaturalNumber() {

        assertThrows(InvalidSizeException.class, () ->
                Allocator.of(GameEvent.class).allocate(0)
        );
    }

    @Test
    public void cantResizeBelowIndex() {
        assertThrows(InvalidSizeException.class, () -> {
            GameEvent event = Allocator.of(GameEvent.class).allocate(5);
            event.move(4);
            event.resize(1);
        });
    }

}
