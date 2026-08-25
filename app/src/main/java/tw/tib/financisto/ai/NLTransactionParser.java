package tw.tib.financisto.ai;

import android.database.Cursor;
import android.util.Log;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.db.DatabaseHelper;
import tw.tib.financisto.model.Account;
import tw.tib.financisto.model.Category;
import tw.tib.financisto.model.Payee;
import tw.tib.orb.EntityManager;

public class NLTransactionParser {
    private static final String TAG = "NLTransactionParser";

    // Known common merchants / payees
    private static final String[] COMMON_PAYEES = {
            "7-11", "7-ELEVEN", "全家", "萊爾富", "OK超商", "全聯", "家樂福", "好市多", "Costco", "大潤發",
            "星巴克", "Starbucks", "路易莎", "Louisa", "麥當勞", "肯德基", "摩斯漢堡", "SUBWAY",
            "Uber Eats", "UberEats", "Foodpanda", "Uber", "台灣大車隊", "中油", "台亞", "全國加油站",
            "屈臣氏", "康是美", "寶雅", "無印良品", "Uniqlo", "NET", "蝦皮", "PChome", "momo",
            "Netflix", "Spotify", "YouTube", "Apple", "Google", "Steam", "Nintendo"
    };

    public static ParsedTransactionResult parse(String input, DatabaseAdapter db) {
        ParsedTransactionResult result = new ParsedTransactionResult();
        if (input == null || input.trim().isEmpty()) {
            return result;
        }

        result.rawInput = input.trim();
        String text = input.trim();

        // 1. Amount Extraction
        extractAmount(text, result);

        // 2. Income vs Expense
        extractIncomeExpense(text, result);

        // 3. Date & Time
        extractDateTime(text, result);

        // 4. Payee
        extractPayee(text, db, result);

        // 5. Account matching
        matchAccount(text, db, result);

        // 6. Category matching
        matchCategory(text, db, result);

        // 7. Note / Description cleanup
        generateNote(text, result);

        Log.d(TAG, "Parsed result: " + result);
        return result;
    }

    private static void extractAmount(String text, ParsedTransactionResult result) {
        // Regex to match currency symbols, numbers, and currency unit keywords
        Pattern pattern = Pattern.compile("(?i)(?:\\$|NT\\$|NTD|TWD|¥|￥|USD)?\\s*([0-9]+(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?)\\s*(?:元|塊|dollars|bucks|NT|TWD)?");
        Matcher matcher = pattern.matcher(text);
        
        double maxFound = 0;
        while (matcher.find()) {
            try {
                String numStr = matcher.group(1).replace(",", "");
                double val = Double.parseDouble(numStr);
                // Exclude common year numbers unless explicitly tagged
                if (val > 1900 && val < 2100 && !matcher.group(0).contains("元") && !matcher.group(0).contains("$") && !matcher.group(0).contains("塊")) {
                    continue;
                }
                if (val > 0) {
                    maxFound = val;
                    // If matched explicit currency suffix/prefix, prefer it
                    if (matcher.group(0).contains("元") || matcher.group(0).contains("塊") || matcher.group(0).contains("$") || matcher.group(0).contains("NT")) {
                        break;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (maxFound > 0) {
            // Financisto standard representation: subunits (scaled by 100 for 2 decimals)
            result.amount = (long) Math.round(maxFound * 100.0);
        }
    }

    private static void extractIncomeExpense(String text, ParsedTransactionResult result) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("收入") || lower.contains("薪資") || lower.contains("薪水") ||
            lower.contains("獎金") || lower.contains("紅利") || lower.contains("股息") ||
            lower.contains("退款") || lower.contains("收到") || lower.contains("進帳") ||
            lower.contains("入帳") || lower.contains("salary") || lower.contains("income") ||
            lower.contains("refund") || lower.contains("bonus") || lower.contains("dividend")) {
            result.isExpense = false;
        } else {
            result.isExpense = true;
        }
    }

    private static void extractDateTime(String text, ParsedTransactionResult result) {
        Calendar cal = Calendar.getInstance();
        String lower = text.toLowerCase(Locale.ROOT);

        if (lower.contains("昨天") || lower.contains("昨日") || lower.contains("yesterday")) {
            cal.add(Calendar.DAY_OF_YEAR, -1);
        } else if (lower.contains("前天")) {
            cal.add(Calendar.DAY_OF_YEAR, -2);
        } else if (lower.contains("大前天")) {
            cal.add(Calendar.DAY_OF_YEAR, -3);
        } else if (lower.contains("上週") || lower.contains("上個禮拜") || lower.contains("上星期")) {
            cal.add(Calendar.DAY_OF_YEAR, -7);
        }

        if (lower.contains("早上") || lower.contains("上午") || lower.contains("morning")) {
            cal.set(Calendar.HOUR_OF_DAY, 8);
            cal.set(Calendar.MINUTE, 30);
        } else if (lower.contains("中午") || lower.contains("午餐") || lower.contains("noon")) {
            cal.set(Calendar.HOUR_OF_DAY, 12);
            cal.set(Calendar.MINUTE, 30);
        } else if (lower.contains("下午") || lower.contains("afternoon")) {
            cal.set(Calendar.HOUR_OF_DAY, 15);
            cal.set(Calendar.MINUTE, 30);
        } else if (lower.contains("晚上") || lower.contains("晚餐") || lower.contains("evening") || lower.contains("night")) {
            cal.set(Calendar.HOUR_OF_DAY, 19);
            cal.set(Calendar.MINUTE, 0);
        } else if (lower.contains("消夜") || lower.contains("宵夜")) {
            cal.set(Calendar.HOUR_OF_DAY, 22);
            cal.set(Calendar.MINUTE, 30);
        }

        result.dateTime = cal.getTimeInMillis();
    }

    private static void extractPayee(String text, DatabaseAdapter db, ParsedTransactionResult result) {
        for (String payee : COMMON_PAYEES) {
            if (text.toLowerCase(Locale.ROOT).contains(payee.toLowerCase(Locale.ROOT))) {
                result.payeeName = payee;
                if (db != null) {
                    try {
                        Payee p = db.findOrInsertEntityByTitle(Payee.class, payee);
                        if (p != null) {
                            result.matchedPayeeId = p.id;
                        }
                    } catch (Exception ignored) {
                    }
                }
                return;
            }
        }
    }

    private static void matchAccount(String text, DatabaseAdapter db, ParsedTransactionResult result) {
        if (db == null) return;
        List<Account> accounts = new ArrayList<>();
        try (Cursor c = db.getAllActiveAccounts()) {
            if (c != null && c.moveToFirst()) {
                do {
                    Account a = EntityManager.loadFromCursor(c, Account.class);
                    if (a != null) accounts.add(a);
                } while (c.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching accounts", e);
        }

        if (accounts.isEmpty()) return;

        String lowerText = text.toLowerCase(Locale.ROOT);

        // 1. Direct title matching
        for (Account a : accounts) {
            if (a.title != null && !a.title.isEmpty() && lowerText.contains(a.title.toLowerCase(Locale.ROOT))) {
                result.accountName = a.title;
                result.matchedAccountId = a.id;
                return;
            }
        }

        // 2. Keyword fuzzy matching
        Map<String, String[]> keywordMap = new HashMap<>();
        keywordMap.put("現金", new String[]{"現金", "cash", "錢包", "皮夾"});
        keywordMap.put("信用卡", new String[]{"信用卡", "credit", "刷卡", "中信", "國泰", "玉山", "台新", "富邦", "聯邦", "花旗", "星展"});
        keywordMap.put("LINE Pay", new String[]{"line pay", "linepay", "一卡通"});
        keywordMap.put("街口", new String[]{"街口", "街口支付", "jkopay"});
        keywordMap.put("悠遊卡", new String[]{"悠遊卡", "easycard"});
        keywordMap.put("銀行", new String[]{"銀行", "轉帳", "匯款", "郵局", "bank"});

        for (Map.Entry<String, String[]> entry : keywordMap.entrySet()) {
            for (String kw : entry.getValue()) {
                if (lowerText.contains(kw)) {
                    // Find an account containing either the key or keyword
                    for (Account a : accounts) {
                        String title = a.title != null ? a.title.toLowerCase(Locale.ROOT) : "";
                        if (title.contains(entry.getKey().toLowerCase(Locale.ROOT)) || title.contains(kw)) {
                            result.accountName = a.title;
                            result.matchedAccountId = a.id;
                            return;
                        }
                    }
                }
            }
        }

        // Default to first active account
        result.accountName = accounts.get(0).title;
        result.matchedAccountId = accounts.get(0).id;
    }

    private static void matchCategory(String text, DatabaseAdapter db, ParsedTransactionResult result) {
        if (db == null) return;
        List<Category> categories = new ArrayList<>();
        try (Cursor c = db.db().query(DatabaseHelper.CATEGORY_TABLE, null, null, null, null, null, "left asc")) {
            if (c != null && c.moveToFirst()) {
                do {
                    Category cat = EntityManager.loadFromCursor(c, Category.class);
                    if (cat != null) categories.add(cat);
                } while (c.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching categories", e);
        }

        if (categories.isEmpty()) return;

        String lowerText = text.toLowerCase(Locale.ROOT);

        // 1. Direct matching with existing category titles
        for (Category cat : categories) {
            if (cat.title != null && cat.title.length() >= 2 && lowerText.contains(cat.title.toLowerCase(Locale.ROOT))) {
                result.categoryName = cat.title;
                result.matchedCategoryId = cat.id;
                return;
            }
        }

        // 2. Keyword taxonomy matching
        String matchedCatKeyword = null;
        if (lowerText.contains("便當") || lowerText.contains("午餐") || lowerText.contains("晚餐") ||
            lowerText.contains("早餐") || lowerText.contains("吃") || lowerText.contains("火鍋") ||
            lowerText.contains("牛排") || lowerText.contains("餐廳") || lowerText.contains("聚餐") ||
            lowerText.contains("食物") || lowerText.contains("飯") || lowerText.contains("麵") ||
            lowerText.contains("麥當勞") || lowerText.contains("肯德基") || lowerText.contains("food")) {
            matchedCatKeyword = "餐飲";
        } else if (lowerText.contains("咖啡") || lowerText.contains("飲料") || lowerText.contains("手搖") ||
                   lowerText.contains("星巴克") || lowerText.contains("奶茶") || lowerText.contains("下午茶") ||
                   lowerText.contains("drink") || lowerText.contains("coffee")) {
            matchedCatKeyword = "飲料";
        } else if (lowerText.contains("高鐵") || lowerText.contains("捷運") || lowerText.contains("公車") ||
                   lowerText.contains("計程車") || lowerText.contains("uber") || lowerText.contains("taxi") ||
                   lowerText.contains("加油") || lowerText.contains("中油") || lowerText.contains("停車") ||
                   lowerText.contains("火車") || lowerText.contains("交通") || lowerText.contains("悠遊卡")) {
            matchedCatKeyword = "交通";
        } else if (lowerText.contains("全聯") || lowerText.contains("家樂福") || lowerText.contains("好市多") ||
                   lowerText.contains("超商") || lowerText.contains("7-11") || lowerText.contains("全家") ||
                   lowerText.contains("買") || lowerText.contains("生活用品") || lowerText.contains("超市") ||
                   lowerText.contains("日用品") || lowerText.contains("購物") || lowerText.contains("shop")) {
            matchedCatKeyword = "購物";
        } else if (lowerText.contains("電影") || lowerText.contains("唱歌") || lowerText.contains("ktv") ||
                   lowerText.contains("遊戲") || lowerText.contains("steam") || lowerText.contains("netflix") ||
                   lowerText.contains("spotify") || lowerText.contains("娛樂")) {
            matchedCatKeyword = "娛樂";
        } else if (lowerText.contains("看病") || lowerText.contains("診所") || lowerText.contains("藥局") ||
                   lowerText.contains("醫藥") || lowerText.contains("醫院") || lowerText.contains("牙醫") ||
                   lowerText.contains("醫療")) {
            matchedCatKeyword = "醫療";
        } else if (lowerText.contains("水費") || lowerText.contains("電費") || lowerText.contains("瓦斯") ||
                   lowerText.contains("房租") || lowerText.contains("管理費") || lowerText.contains("電信") ||
                   lowerText.contains("電話費") || lowerText.contains("網路費")) {
            matchedCatKeyword = "居家";
        } else if (lowerText.contains("薪水") || lowerText.contains("薪資") || lowerText.contains("獎金") ||
                   lowerText.contains("股息") || lowerText.contains("收入") || lowerText.contains("salary")) {
            matchedCatKeyword = "薪資";
        }

        if (matchedCatKeyword != null) {
            for (Category cat : categories) {
                if (cat.title != null && (cat.title.contains(matchedCatKeyword) || matchedCatKeyword.contains(cat.title))) {
                    result.categoryName = cat.title;
                    result.matchedCategoryId = cat.id;
                    return;
                }
            }
        }
    }

    private static void generateNote(String text, ParsedTransactionResult result) {
        String clean = text;
        // Strip amount keywords
        clean = clean.replaceAll("(?i)(?:\\$|NT\\$|NTD|TWD|¥|￥|USD)?\\s*[0-9]+(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?\\s*(?:元|塊|dollars|bucks|NT|TWD)?", "");
        // Strip time keywords
        clean = clean.replaceAll("(今天|昨天|前天|大前天|上週|上星期|早上|上午|中午|下午|晚上|消夜|宵夜|today|yesterday)", "");
        // Strip payment method keywords
        clean = clean.replaceAll("(用現金|付現|現金|刷卡|用信用卡|信用卡|LINE Pay|linepay|街口支付|街口|悠遊卡|轉帳|付款|支付|買了|買包|買杯|買瓶|吃了一頓|吃了|在|用)", "");
        clean = clean.replaceAll("[，,。!！?？\\s]+", " ").trim();

        if (clean.isEmpty()) {
            if (!result.payeeName.isEmpty()) {
                clean = result.payeeName;
            } else if (!result.categoryName.isEmpty()) {
                clean = result.categoryName;
            } else {
                clean = text;
            }
        }
        result.note = clean;
    }
}
