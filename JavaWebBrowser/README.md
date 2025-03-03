## mvn javafx:run

# Семестр 1. Лабораторная работа №7.

"Веб-браузер". Программа должна предоставлять возможность просмотра html-страниц online(через протокол HTTP). Хранить историю с указанием дат и временем проведенным на сайтах(хранить историю необходимо в файлах формата JSON или XML). Добавить возможность отключения истории для всех или определенных сайтов. Реализовать возможность сохранения страниц сайтов и их сжатие по механизму ZIP. Реализовать возможность просмотра и редактирования html-кода страницы в соседнем окне. Реализовать механизм вкладок и их параллельное выполнение. Реализовать возможность создания собственных html – страниц(минимальное редактирование и сохранение файлов html). Реализовать возможность добавлять сайты в папку “Избранное”. В браузере обязательно должны присутствовать кнопки “возврата по истории” и “вперед по истории”, а также кнопка обновить страницу. Должно присутствовать поле для ввода полного адреса сайта. 

Пользовательский интерфейс для программы обязателен.

Процесс сдачи лабораторной работы:
1. Клонируете репозиторий
![github-1](https://github.com/new94/JavaServiceBrackets/assets/3996014/79ae3da4-cfc6-4fe1-ae8f-36cea470993b)
2. Переходите в ветку develop (checkout)
3. На основе ветки develop создаёте свою ветку с названием по шаблону student/номергруппы_фамилия_перваябукваимени
4. Пишите код в своей ветке student/номергруппы_фамилия_перваябукваимени
5. Проверяйте код тестами
![github-2](https://github.com/new94/JavaServiceBrackets/assets/3996014/7eb73962-ef01-4e0a-bcb0-a05dd1406d01)
6. Если все тесты пройдены, то можно отправлять код на проверку, для этого нужно создать Pull Request
7. В репозитории в github перейдите во вкладку Pull Requests
8. Выберите в base ветку develop, а в compare свою ветку, например (student/0000_nenakhov_e)
![github-3](https://github.com/new94/JavaServiceBrackets/assets/3996014/eb7c329c-1581-4a0f-ab5b-79ff3061e6d4)
9. Нажимаете create pull request
10. Далее выбираете в Reviewers справа new94 (Ненахов Евгений)
11. Далее выбираете в Assignees справа new94 и себя
12. В описании Pull Request пишите "Фамилия Имя - лабороторная работа", например "Ненахов Евгений - лабораторная работа"
![image](https://github.com/new94/JavaServiceBrackets/assets/3996014/ed1553d6-1d41-41f2-844a-c24a3f69ca85)
13. Нажимаете create pull request
14. После создания pull request запускается автоматическая проверка тестов. Все тесты должны быть пройдены. Если тесты не пройдены, лабораторная работа проверяться не будет. Чтобы перезапустить автоматическую проверку, нужно переоткрыть pull request. 
15. Ненахов Евгений смотрит код и оставляет комментарии к коду. Все комментарии нужно либо поправить, либо ответить, но закрывать pull request нельзя!
16. Если лабораторная работа не сдана, то будет комментарий от Ненахов Евгений о том, что нужно поправить, чтобы сдать
17. Если с кодом всё хорошо, то будет комментарий, что лабораторная работа сдана.
18. Делать commit и push в любые ветки, кроме своей строго запрещено!
