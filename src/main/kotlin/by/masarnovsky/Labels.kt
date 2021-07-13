package by.masarnovsky


const val REPAY_VALUE = "Возврат суммы"
const val MAIN_MENU_DESCRIPTION =
    "Добавляй должника в таком формате: \n<b>имя 66.6 комментарий</b> \nЧтобы вычесть сумму долга: \n<b>имя -97</b> \nКнопочка чтобы посмотреть всех"
const val NEGATIVE_BALANCE_ERROR = "Введена неверная сумма, баланс не может быть отрицательным"
const val COMMON_ERROR = "Произошла ошибка. Пожалуйста, повторите запрос"
const val NOT_DELETE_HISTORY = "Вы решили не удалять историю"
const val DEBTOR_RECORD = "Долг %s равен %.2f BYN за: <b>%s</b>\n<i>Посмотреть всех:</i> /all"
const val DEBTOR_RECORD_FOR_INLINE_QUERY = "Долг %s равен %.2f BYN"
const val DEBTOR_RECORD_SHORT = "%s %.2f BYN за: <b>%s</b>"
const val SUCCESSFUL_DEBTOR_REMOVAL = "Информация о должнике %s была удалена"
const val SUCCESSFUL_DEBTORS_REMOVAL = "Информация о %d должниках была удалена"
const val DEBTOR_NOT_FOUND = "По такому имени ничего не найдено"
const val DEBTORS_NOT_FOUND = "Должников не найдено"
const val DELETE_ALL_DEBTORS_WARNING = "Вы точно хотите удалить <b>всех</b> должников?"
const val DEBTOR_LOG_HISTORY_HEADER = "Текущий долг для %s равняется %.2f BYN\nИстория долгов:\n"
const val LOG_SUMMARIZE = "%s |   %.2f за %s\n"
const val YES = "Да"
const val NO = "Нет"
const val LIST_OF_ALL = "Список всех"
const val ALL_DEBTS_TOTAL_AMOUNT = "Общая сумма долгов равна %.2f BYN\n"
const val HOWTO_INFO = """
Добавляй должника в таком формате: 
<b>имя 66.6 комментарий</b> 
Чтобы вычесть сумму долга: 
<b>имя -97</b>    
    
Список доступных комманд:
Показать историю для конкретного должника:
<b>/show имя</b>
Без параметров будет выведен список всех должников:
<b>/show</b>
Список всех должников:
<b>/all</b>
Удалить запись о должнике:
<b>/delete имя</b> 
Без параметров будут удалены <b>все</b> должники:
<b>/delete</b> 
"""



