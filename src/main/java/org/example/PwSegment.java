package org.example;

import java.util.UUID;

enum SegmentType { LINEAR, CONSTANT, EXPR }

final class PwSegment {
    double a, b;
    boolean inclA, inclB;
    SegmentType type;
    boolean enabled = true;

    // LINEAR
    double m, c;          // y = m*x + c

    // CONSTANT
    double value;

    // EXPR
    String expr;          // z.B. "sin(x)+0.3*x"

    // Optionales Clamp
    boolean clamp = false;
    double yMin, yMax;

    // Hilfsname/ID fürs GUI (löschen, umbenennen usw.)
    String id = UUID.randomUUID().toString();

    String toExpressionString(String multiplyWith) {
        String base;
        switch (type) {
            case LINEAR:
                base = String.format(java.util.Locale.US, "%.17g*x + (%.17g)", m, c);
                break;
            case CONSTANT:
                base = String.format(java.util.Locale.US, "(%.17g)", value);
                break;
            case EXPR:
                base = "(" + expr + ")";
                break;
            default:
                throw new IllegalStateException();
        }
        if (clamp) {
            base = String.format(java.util.Locale.US,
                    "min(max((%s), %.17g), %.17g)", base, yMin, yMax);
        }
        if (multiplyWith != null && !multiplyWith.isEmpty()) {
            base = "(" + multiplyWith + ")*(" + base + ")";
        }
        return base;
    }
}
