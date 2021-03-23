package uk.lgl.modmenu;

import android.graphics.Canvas;

interface ESPViewInternalDelegate {
    void DrawCircle(Canvas canvas, String str, float f, float f2, float f3, float f4);

    void DrawFilledCircle(Canvas canvas, String str, float f, float f2, float f3);

    void DrawFilledRect(Canvas canvas, String str, float f, float f2, float f3, float f4);

    void DrawLine(Canvas canvas, String str, float f, float f2, float f3, float f4, float f5);

    void DrawRect(Canvas canvas, String str, int i, float f, float f2, float f3, float f4);

    void DrawText(Canvas canvas, String str, String str2, float f, float f2, float f3);
}