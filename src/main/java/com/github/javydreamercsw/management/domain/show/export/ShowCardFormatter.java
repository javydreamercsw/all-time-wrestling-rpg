package com.github.javydreamercsw.management.domain.show.export;

import com.github.javydreamercsw.management.domain.show.Show;

/**
 * Interface for formatting a show card for export.
 */
public interface ShowCardFormatter {

    /**
     * Get the display name of the export format.
     *
     * @return display name
     */
    String getFormatName();

    /**
     * Format the show card.
     *
     * @param show the show to format
     * @return formatted text
     */
    String format(Show show);

    /**
     * Get the priority for sorting in the UI. Lower values come first.
     *
     * @return priority
     */
    default int getPriority() {
        return 100;
    }
}
