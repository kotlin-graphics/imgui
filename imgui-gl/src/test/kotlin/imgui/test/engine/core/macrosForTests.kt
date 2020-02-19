package imgui.test.engine.core

//-------------------------------------------------------------------------
// Macros for Tests
//-------------------------------------------------------------------------

// We embed every macro in a do {} while(0) statement as a trick to allow using them as regular single statement, e.g. if (XXX) IM_CHECK(A); else IM_CHECK(B)
// We leave the assert call (which will trigger a debugger break) outside of the check function to step out faster.
fun CHECK_NO_RET(expr: Boolean) {
    if (TestEngineHook_Check(/*__FILE__, __func__, __LINE__,*/ TestCheckFlag.None.i, expr))
        assert(expr)
}

fun CHECK(expr: Boolean) {
    if (TestEngineHook_Check(/*__FILE__, __func__, __LINE__,*/ TestCheckFlag.None.i, expr))
        assert(expr)
    if (!expr) return
}
fun CHECK_SILENT(expr: Boolean) {
    if (TestEngineHook_Check(/*__FILE__, __func__, __LINE__,*/ TestCheckFlag.SilentSuccess.i, expr))
        assert(false)
    if (!expr) return
}
fun <R>CHECK_RETV(expr: Boolean, ret: R): R? {
    if (TestEngineHook_Check(/*__FILE__, __func__, __LINE__,*/ TestCheckFlag.None.i, expr))
        assert(expr)
    return if (!expr) ret else null
}
//#define IM_ERRORF(_FMT,...)         do {
//    if (ImGuiTestEngineHook_Error(__FILE__, __func__, __LINE__, ImGuiTestCheckFlags_None, _FMT, __VA_ARGS__)) {
//        IM_ASSERT(0); }
//} while (0)
fun ERRORF_NOHDR(fmt: String, vararg args: Any) {
    if (TestEngineHook_Error(/*NULL, NULL, 0,*/ TestCheckFlag.None.i, fmt, args))
        assert(false)
}

//template<typename T> void ImGuiTestEngineUtil_AppendStrValue(ImGuiTextBuffer& buf, T value)         { buf.append("???"); IM_UNUSED(value); }
//template<> inline void ImGuiTestEngineUtil_AppendStrValue(ImGuiTextBuffer& buf, const char* value)  { buf.appendf("%s", value); }
//template<> inline void ImGuiTestEngineUtil_AppendStrValue(ImGuiTextBuffer& buf, bool value)         { buf.append(value ? "true" : "false"); }
//template<> inline void ImGuiTestEngineUtil_AppendStrValue(ImGuiTextBuffer& buf, int value)          { buf.appendf("%d", value); }
//template<> inline void ImGuiTestEngineUtil_AppendStrValue(ImGuiTextBuffer& buf, unsigned int value) { buf.appendf("%u", value); }
//template<> inline void ImGuiTestEngineUtil_AppendStrValue(ImGuiTextBuffer& buf, ImS8 value)         { buf.appendf("%d", value); }
//template<> inline void ImGuiTestEngineUtil_AppendStrValue(ImGuiTextBuffer& buf, ImU8 value)         { buf.appendf("%u", value); }
//template<> inline void ImGuiTestEngineUtil_AppendStrValue(ImGuiTextBuffer& buf, ImS16 value)        { buf.appendf("%hd", value); }
//template<> inline void ImGuiTestEngineUtil_AppendStrValue(ImGuiTextBuffer& buf, ImU16 value)        { buf.appendf("%hu", value); }
//template<> inline void ImGuiTestEngineUtil_AppendStrValue(ImGuiTextBuffer& buf, float value)        { buf.appendf("%f", value); }
//template<> inline void ImGuiTestEngineUtil_AppendStrValue(ImGuiTextBuffer& buf, double value)       { buf.appendf("%f", value); }
//template<> inline void ImGuiTestEngineUtil_AppendStrValue(ImGuiTextBuffer& buf, ImVec2 value)       { buf.appendf("(%f, %f)", value.x, value.y); }
//template<> inline void ImGuiTestEngineUtil_AppendStrValue(ImGuiTextBuffer& buf, const void* value)  { buf.appendf("%p", value); }
//
//// Those macros allow us to print out the values of both lhs and rhs expressions involved in a check.
//#define IM_CHECK_OP_NO_RET(_LHS, _RHS, _OP)                                 \
//do                                                                      \
//{
//    \
//    auto __lhs = _LHS;  /* Cache in variables to avoid side effects */  \
//    auto __rhs = _RHS; \
//    bool __res = __lhs _OP __rhs; \
//    ImGuiTextBuffer value_expr_buf; \
//    if (!__res)                                                         \
//    {
//        \
//        ImGuiTestEngineUtil_AppendStrValue(value_expr_buf, __lhs); \
//        value_expr_buf.append(" " # _OP " ");                            \
//        ImGuiTestEngineUtil_AppendStrValue(value_expr_buf, __rhs); \
//    }                                                                   \
//    if (ImGuiTestEngineHook_Check(__FILE__, __func__, __LINE__, ImGuiTestCheckFlags_None, __res, # _LHS " " #_OP " " #_RHS, value_expr_buf.c_str())) \
//    IM_ASSERT(__res); \
//} while (0)
//#define IM_CHECK_OP(_LHS, _RHS, _OP)                                        \
//do                                                                      \
//{
//    \
//    auto __lhs = _LHS;  /* Cache in variables to avoid side effects */  \
//    auto __rhs = _RHS; \
//    bool __res = __lhs _OP __rhs; \
//    ImGuiTextBuffer value_expr_buf; \
//    if (!__res)                                                         \
//    {
//        \
//        ImGuiTestEngineUtil_AppendStrValue(value_expr_buf, __lhs); \
//        value_expr_buf.append(" " # _OP " ");                            \
//        ImGuiTestEngineUtil_AppendStrValue(value_expr_buf, __rhs); \
//    }                                                                   \
//    if (ImGuiTestEngineHook_Check(__FILE__, __func__, __LINE__, ImGuiTestCheckFlags_None, __res, # _LHS " " #_OP " " #_RHS, value_expr_buf.c_str())) \
//    IM_ASSERT(__res); \
//    if (!__res)                                                         \
//    return; \
//} while (0)
//
//#define IM_CHECK_EQ(_LHS, _RHS)         IM_CHECK_OP(_LHS, _RHS, ==)         // Equal
//#define IM_CHECK_NE(_LHS, _RHS)         IM_CHECK_OP(_LHS, _RHS, !=)         // Not Equal
//#define IM_CHECK_LT(_LHS, _RHS)         IM_CHECK_OP(_LHS, _RHS, <)          // Less Than
//#define IM_CHECK_LE(_LHS, _RHS)         IM_CHECK_OP(_LHS, _RHS, <=)         // Less or Equal
//#define IM_CHECK_GT(_LHS, _RHS)         IM_CHECK_OP(_LHS, _RHS, >)          // Greater Than
//#define IM_CHECK_GE(_LHS, _RHS)         IM_CHECK_OP(_LHS, _RHS, >=)         // Greater or Equal
//
//#define IM_CHECK_EQ_NO_RET(_LHS, _RHS)  IM_CHECK_OP_NO_RET(_LHS, _RHS, ==)  // Equal
//#define IM_CHECK_NE_NO_RET(_LHS, _RHS)  IM_CHECK_OP_NO_RET(_LHS, _RHS, !=)  // Not Equal
//#define IM_CHECK_LT_NO_RET(_LHS, _RHS)  IM_CHECK_OP_NO_RET(_LHS, _RHS, <)   // Less Than
//#define IM_CHECK_LE_NO_RET(_LHS, _RHS)  IM_CHECK_OP_NO_RET(_LHS, _RHS, <=)  // Less or Equal
//#define IM_CHECK_GT_NO_RET(_LHS, _RHS)  IM_CHECK_OP_NO_RET(_LHS, _RHS, >)   // Greater Than
//#define IM_CHECK_GE_NO_RET(_LHS, _RHS)  IM_CHECK_OP_NO_RET(_LHS, _RHS, >=)  // Greater or Equal
//
//#define IM_CHECK_STR_EQ(_LHS, _RHS)                                         \
//do                                                                      \
//{
//    \
//    bool __res = strcmp (_LHS, _RHS) == 0;                               \
//    ImGuiTextBuffer value_expr_buf; \
//    if (!__res)                                                         \
//    {
//        \
//        value_expr_buf.appendf("\"%s\" == \"%s\"", _LHS, _RHS); \
//    }                                                                   \
//    if (ImGuiTestEngineHook_Check(__FILE__, __func__, __LINE__, ImGuiTestCheckFlags_None, __res, # _LHS " == " #_RHS, value_expr_buf.c_str())) \
//    IM_ASSERT(__res); \
//    if (!__res)                                                         \
//    return; \
//} while (0)
//
////#define IM_ASSERT(_EXPR)      (void)( (!!(_EXPR)) || (ImGuiTestEngineHook_Check(false, #_EXPR, __FILE__, __func__, __LINE__), 0) )

//-------------------------------------------------------------------------
// [SECTION] HOOKS FOR TESTS
//-------------------------------------------------------------------------
// - ImGuiTestEngineHook_Check()
// - ImGuiTestEngineHook_Error()
//-------------------------------------------------------------------------

// Return true to request a debugger break
fun TestEngineHook_Check(/*file: String? = null, func: String = "", line: Int,*/
        flags: TestCheckFlags, result: Boolean, expr: String? = null): Boolean {

    val engine = hookingEngine

    // Removed absolute path from output so we have deterministic output (otherwise __FILE__ gives us machine depending output)
//    val fileWithoutPath = file ? ImPathFindFilename(file) : ""

    val ctx = engine!!.testContext
    if (ctx != null) {
        val test = ctx.test!!
        //ctx->LogDebug("IM_CHECK(%s)", expr);
        if (!result) {
            if (ctx.runFlags hasnt TestRunFlag.NoTestFunc)
                test.status = TestStatus.Error

            val sf = StackWalker.getInstance().walk { it.findFirst().get() }
            System.err.printf("KO Class: ${sf.declaringClass.simpleName}, Method: %-7s, Line: ${sf.lineNumber}%n", sf.methodName)
//            val display_value_expr = (value_expr != NULL) && (result == false)
//            if (file) {
//                if (display_value_expr)
//                    ctx->LogError("KO %s:%d '%s' -> '%s'", file_without_path, line, expr, value_expr)
//                else
//                ctx->LogError("KO %s:%d '%s'", file_without_path, line, expr)
//            } else {
//                if (display_value_expr)
//                    ctx->LogError("KO '%s' -> '%s'", expr, value_expr)
//                else
//                ctx->LogError("KO '%s'", expr)
//            }
        } else if (flags hasnt TestCheckFlag.SilentSuccess) {
            val sf = StackWalker.getInstance().walk { it.findFirst().get() }
            System.err.printf("OK Class: ${sf.declaringClass.simpleName}, Method: %-7s, Line: ${sf.lineNumber}%n", sf.methodName)
//            if (file)
//                ctx->LogInfo("OK %s:%d '%s'", file_without_path, line, expr)
//            else
//            ctx->LogInfo("OK '%s'", expr)
        }
    }
//    else {
//            ctx.logError("Error: no active test!\n")
//            assert(false)
//        }

    if (result == false && engine.io.configStopOnError && !engine.abort)
        engine.abort = true
    //ImGuiTestEngine_Abort(engine);

    if (result == false && engine.io.configBreakOnError && !engine.abort)
        return true

    return false
}

fun TestEngineHook_Error(/*file, const char* func, int line,*/ flags: TestCheckFlags, fmt: String, vararg args: Any): Boolean{
    val buf = fmt.format(args)
    val ret = TestEngineHook_Check (/*file, func, line,*/ flags, false, buf)

    val engine = hookingEngine
    return when (engine?.abort) {
        true -> false
        else -> ret
    }
}