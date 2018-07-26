package imgui.imgui.demo.showExampleApp

import glm_.vec2.Vec2
import imgui.Cond
import imgui.ImGui.begin_
import imgui.ImGui.end
import imgui.ImGui.setNextWindowSize
import imgui.ImGui.text
import kotlin.reflect.KMutableProperty0
import imgui.ColorEditFlag as Cef
import imgui.InputTextFlag as Itf
import imgui.SelectableFlag as Sf
import imgui.TreeNodeFlag as Tnf
import imgui.WindowFlag as Wf

object CustomRendering {

    /** Demonstrate using the low-level ImDrawList to draw custom shapes.   */
    operator fun invoke(open: KMutableProperty0<Boolean>) {

        setNextWindowSize(Vec2(350, 560), Cond.FirstUseEver)
        if (!begin_("Example: Custom rendering", open)) {
            end()
            return
        }

        text("TODO")

        /*  Tip: If you do a lot of custom rendering, you probably want to use your own geometrical types and
            benefit of overloaded operators, etc.
            Define IM_VEC2_CLASS_EXTRA in imconfig.h to create implicit conversions between your types and
            ImVec2/ImVec4.
            ImGui defines overloaded operators but they are internal to imgui.cpp and not exposed outside
            (to avoid messing with your types)
            In this example we are not using the maths operators!   */
//            ImDrawList* draw_list = ImGui::GetWindowDrawList();
//
//            // Primitives
//            ImGui::Text("Primitives");
//            static float sz = 36.0f;
//            static float thickness = 4.0f;
//            static ImVec4 col = ImVec4(1.0f,1.0f,0.4f,1.0f);
//            ImGui::DragFloat("Size", &sz, 0.2f, 2.0f, 72.0f, "%.0f");
//            ImGui::DragFloat("Thickness", &thickness, 0.05f, 1.0f, 8.0f, "%.02f");
//            ImGui::ColorEdit3("Color", &col.x);
//            {
//                const ImVec2 p = ImGui::GetCursorScreenPos();
//                const ImU32 col32 = ImColor(col);
//                float x = p.x + 4.0f, y = p.y + 4.0f, spacing = 8.0f;
//                for (int n = 0; n < 2; n++)
//                {
//                    float curr_thickness = (n == 0) ? 1.0f : thickness;
//                    draw_list->AddCircle(ImVec2(x+sz*0.5f, y+sz*0.5f), sz*0.5f, col32, 20, curr_thickness); x += sz+spacing;
//                    draw_list->AddRect(ImVec2(x, y), ImVec2(x+sz, y+sz), col32, 0.0f, ImDrawCornerFlags_All, curr_thickness); x += sz+spacing;
//                    draw_list->AddRect(ImVec2(x, y), ImVec2(x+sz, y+sz), col32, 10.0f, ImDrawCornerFlags_All, curr_thickness); x += sz+spacing;
//                    draw_list->AddRect(ImVec2(x, y), ImVec2(x+sz, y+sz), col32, 10.0f, ImDrawCornerFlags_TopLeft|ImDrawCornerFlags_BotRight, curr_thickness); x += sz+spacing;
//                    draw_list->AddTriangle(ImVec2(x+sz*0.5f, y), ImVec2(x+sz,y+sz-0.5f), ImVec2(x,y+sz-0.5f), col32, curr_thickness); x += sz+spacing;
//                    draw_list->AddLine(ImVec2(x, y), ImVec2(x+sz, y   ), col32, curr_thickness); x += sz+spacing; // Horizontal line (note: drawing a filled rectangle will be faster!)
//                    draw_list->AddLine(ImVec2(x, y), ImVec2(x,    y+sz), col32, curr_thickness); x += spacing;    // Vertical line (note: drawing a filled rectangle will be faster!)
//                    draw_list->AddLine(ImVec2(x, y), ImVec2(x+sz, y+sz), col32, curr_thickness); x += sz+spacing; // Diagonal line
//                    draw_list->AddBezierCurve(ImVec2(x, y), ImVec2(x+sz*1.3f,y+sz*0.3f), ImVec2(x+sz-sz*1.3f,y+sz-sz*0.3f), ImVec2(x+sz, y+sz), col32, curr_thickness);
//                    x = p.x + 4;
//                    y += sz+spacing;
//                }
//                draw_list->AddCircleFilled(ImVec2(x+sz*0.5f, y+sz*0.5f), sz*0.5f, col32, 32); x += sz+spacing;
//                draw_list->AddRectFilled(ImVec2(x, y), ImVec2(x+sz, y+sz), col32); x += sz+spacing;
//                draw_list->AddRectFilled(ImVec2(x, y), ImVec2(x+sz, y+sz), col32, 10.0f); x += sz+spacing;
//                draw_list->AddRectFilled(ImVec2(x, y), ImVec2(x+sz, y+sz), col32, 10.0f, ImDrawCornerFlags_TopLeft|ImDrawCornerFlags_BotRight); x += sz+spacing;
//                draw_list->AddTriangleFilled(ImVec2(x+sz*0.5f, y), ImVec2(x+sz,y+sz-0.5f), ImVec2(x,y+sz-0.5f), col32); x += sz+spacing;
//                draw_list->AddRectFilled(ImVec2(x, y), ImVec2(x+sz, y+thickness), col32); x += sz+spacing;          // Horizontal line (faster than AddLine, but only handle integer thickness)
//                draw_list->AddRectFilled(ImVec2(x, y), ImVec2(x+thickness, y+sz), col32); x += spacing+spacing;     // Vertical line (faster than AddLine, but only handle integer thickness)
//                draw_list->AddRectFilled(ImVec2(x, y), ImVec2(x+1, y+1), col32);          x += sz;                  // Pixel (faster than AddLine)
//                draw_list->AddRectFilledMultiColor(ImVec2(x, y), ImVec2(x+sz, y+sz), IM_COL32(0,0,0,255), IM_COL32(255,0,0,255), IM_COL32(255,255,0,255), IM_COL32(0,255,0,255));
//                ImGui::Dummy(ImVec2((sz+spacing)*8, (sz+spacing)*3));
//            }
//            ImGui::Separator();
//            {
//                static ImVector<ImVec2> points;
//                static bool adding_line = false;
//                ImGui::Text("Canvas example");
//                if (ImGui::Button("Clear")) points.clear();
//                if (points.Size >= 2) { ImGui::SameLine(); if (ImGui::Button("Undo")) { points.pop_back(); points.pop_back(); } }
//                ImGui::Text("Left-click and drag to add lines,\nRight-click to undo");
//
//                // Here we are using InvisibleButton() as a convenience to 1) advance the cursor and 2) allows us to use IsItemHovered()
//                // But you can also draw directly and poll mouse/keyboard by yourself. You can manipulate the cursor using GetCursorPos() and SetCursorPos().
//                // If you only use the ImDrawList API, you can notify the owner window of its extends by using SetCursorPos(max).
//                ImVec2 canvas_pos = ImGui::GetCursorScreenPos();            // ImDrawList API uses screen coordinates!
//                ImVec2 canvas_size = ImGui::GetContentRegionAvail();        // Resize canvas to what's available
//                if (canvas_size.x < 50.0f) canvas_size.x = 50.0f;
//                if (canvas_size.y < 50.0f) canvas_size.y = 50.0f;
//                draw_list->AddRectFilledMultiColor(canvas_pos, ImVec2(canvas_pos.x + canvas_size.x, canvas_pos.y + canvas_size.y), IM_COL32(50,50,50,255), IM_COL32(50,50,60,255), IM_COL32(60,60,70,255), IM_COL32(50,50,60,255));
//                draw_list->AddRect(canvas_pos, ImVec2(canvas_pos.x + canvas_size.x, canvas_pos.y + canvas_size.y), IM_COL32(255,255,255,255));
//
//                bool adding_preview = false;
//                ImGui::InvisibleButton("canvas", canvas_size);
//                ImVec2 mouse_pos_in_canvas = ImVec2(ImGui::GetIO().MousePos.x - canvas_pos.x, ImGui::GetIO().MousePos.y - canvas_pos.y);
//                if (adding_line)
//                {
//                    adding_preview = true;
//                    points.push_back(mouse_pos_in_canvas);
//                    if (!ImGui::IsMouseDown[0])
//                        adding_line = adding_preview = false;
//                }
//                if (ImGui::IsItemHovered())
//                {
//                    if (!adding_line && ImGui::IsMouseClicked(0))
//                    {
//                        points.push_back(mouse_pos_in_canvas);
//                        adding_line = true;
//                    }
//                    if (ImGui::IsMouseClicked(1) && !points.empty())
//                    {
//                        adding_line = adding_preview = false;
//                        points.pop_back();
//                        points.pop_back();
//                    }
//                }
//                draw_list->PushClipRect(canvas_pos, ImVec2(canvas_pos.x+canvas_size.x, canvas_pos.y+canvas_size.y), true);      // clip lines within the canvas (if we resize it, etc.)
//                for (int i = 0; i < points.Size - 1; i += 2)
//                draw_list->AddLine(ImVec2(canvas_pos.x + points[i].x, canvas_pos.y + points[i].y), ImVec2(canvas_pos.x + points[i+1].x, canvas_pos.y + points[i+1].y), IM_COL32(255,255,0,255), 2.0f);
//                draw_list->PopClipRect();
//                if (adding_preview)
//                    points.pop_back();
//            }
        end()
    }
}