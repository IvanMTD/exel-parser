package ru.morgan.exelparser.configuration;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.morgan.exelparser.models.*;
import ru.morgan.exelparser.repositories.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Configuration
public class DatabasePreConfiguration {
    @Bean
    public CommandLineRunner dataLoader(
            TypeOfSportRepository sportRepository,
            DisciplineRepository disciplineRepository,
            SubjectRepository subjectRepository,
            ParticipantRepository participantRepository,
            QualificationRepository qualificationRepository,
            AgeGroupRepository groupRepository
    ){
        return new CommandLineRunner() {
            @Override
            public void run(String... args) throws Exception {
                //addSportAndDiscipline(sportRepository,disciplineRepository);
                //addSeasonsAndFiltersInSport(sportRepository);
                //createSubjectAndAddInSport(subjectRepository,sportRepository);
                //addParticipantAndQualification(sportRepository,disciplineRepository,subjectRepository,participantRepository,qualificationRepository,groupRepository);
            }
        };
    }

    private void addParticipantAndQualification(
            TypeOfSportRepository sportRepository,
            DisciplineRepository disciplineRepository,
            SubjectRepository subjectRepository,
            ParticipantRepository participantRepository,
            QualificationRepository qualificationRepository,
            AgeGroupRepository groupRepository
    ){
        System.out.println("start process");
        XSSFWorkbook wb = getWorkBookFromXSSF("./src/main/resources/static/file/Participant.xlsx");
        XSSFSheet sheet = wb.getSheet("Лист1");
        Iterator<Row> rowIter = sheet.rowIterator();

        //addParticipant(subjectRepository,participantRepository,rowIter);
        //addAgeGroup(sportRepository,disciplineRepository,groupRepository,rowIter);

        String currentSport = "";
        String currentDiscipline = "";
        String currentGroup = "";
        while (rowIter.hasNext()){
            Row row = rowIter.next();
            Cell sportCell = row.getCell(0);
            Cell groupCell = row.getCell(1);
            Cell disciplineCell = row.getCell(2);
            Cell categoryCell = row.getCell(4);
            Cell birthdayCell = row.getCell(5);
            Cell lastnameCell = row.getCell(6);
            Cell nameCell = row.getCell(7);
            if(sportCell != null){
                currentSport = sportCell.toString();
            }
            if(disciplineCell != null){
                currentDiscipline = disciplineCell.toString();
            }
            if(groupCell != null){
                currentGroup = groupCell.toString();
            }
            String categoryTitle = "";
            if(categoryCell != null){
                categoryTitle = categoryCell.toString();
            }
            String birthdayInfo = "";
            if(birthdayCell != null){
                birthdayInfo = birthdayCell.toString();
            }
            String lastname = "";
            if(lastnameCell != null){
                lastname = lastnameCell.toString();
            }
            String name = "";
            if(nameCell != null){
                name = nameCell.toString();
            }

            if(!categoryTitle.equals("")){
                Category category = parseCategory(categoryTitle);
                Participant participant = null;
                LocalDate birthday = parseLocalDate(birthdayInfo);
                if(birthday != null && !lastname.equals("") && !name.equals("")){
                    participant = participantRepository.findByLastnameAndNameAndBirthday(lastname,name,birthday);
                }
                if(participant != null){
                    if(!currentSport.equals("") && !currentDiscipline.equals("") && !currentGroup.equals("")){
                        TypeOfSport sport = sportRepository.findByTitle(currentSport);
                        if(sport != null) {
                            List<Discipline> disciplines = disciplineRepository.findAllByIdIn(sport.getDisciplineIds());
                            String finalCurrentDiscipline = currentDiscipline;
                            Optional<Discipline> disciplineOptional = disciplines.stream().filter(d -> d.getTitle().equals(finalCurrentDiscipline)).findAny();
                            if (disciplineOptional.isPresent()) {
                                Discipline discipline = disciplineOptional.get();
                                List<AgeGroup> ageGroups = groupRepository.findAllByIdIn(discipline.getAgeGroupIds());
                                String finalCurrentGroup = currentGroup;
                                Optional<AgeGroup> ageGroupOptional = ageGroups.stream().findAny().filter(g -> g.getTitle().equals(finalCurrentGroup)).stream().findAny();
                                if (ageGroupOptional.isPresent()) {
                                    AgeGroup ageGroup = ageGroupOptional.get();
                                    Qualification qualification = new Qualification();
                                    qualification.setCategory(category);
                                    qualification.setParticipantId(participant.getId());
                                    qualification.setAgeGroupId(ageGroup.getId());
                                    qualification = qualificationRepository.save(qualification);
                                    ageGroup.addQualification(qualification);
                                    participant.addQualification(qualification);
                                    ageGroup = groupRepository.save(ageGroup);
                                    participant = participantRepository.save(participant);
                                    System.out.println(participant.getFullName() + " | " + sport.getTitle() + " | " + discipline.getTitle() + " | " + ageGroup.getTitle() + " | " + qualification.getCategory().getTitle());
                                }
                            }
                        }
                    }
                }
            }
        }

        try {
            wb.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("end process");
    }

    private void addAgeGroup(TypeOfSportRepository sportRepository, DisciplineRepository disciplineRepository, AgeGroupRepository groupRepository, Iterator<Row> rowIter){
        String currentSport = "";
        String currentGroup = "";
        Map<String,Map<String,List<String>>> sportMap = new HashMap<>();
        while (rowIter.hasNext()){
            Row row = rowIter.next();
            String sportTitle = getFirstCharInAppearCase(row.getCell(0).toString());
            String groupTitle = row.getCell(1).toString();
            String disciplineTitle = row.getCell(2).toString();
            // Обновляем текущий вид спорта
            if(!sportTitle.equals("")){
                currentSport = sportTitle;
            }
            if(!groupTitle.equals("")){
                currentGroup = groupTitle;
            }

            if(!disciplineTitle.equals("")){
                TypeOfSport sport = sportRepository.findByTitle(currentSport);
                if(sport != null){
                    List<Discipline> disciplines = disciplineRepository.findAllByIdIn(sport.getDisciplineIds());
                    if(disciplines.stream().anyMatch(d -> d.getTitle().equals(disciplineTitle))){
                        if(sportMap.get(currentSport) != null){
                            if(sportMap.get(currentSport).get(disciplineTitle) != null){
                                sportMap.get(currentSport).get(disciplineTitle).add(currentGroup);
                            }else{
                                sportMap.get(currentSport).put(disciplineTitle, new ArrayList<>());
                                sportMap.get(currentSport).get(disciplineTitle).add(currentGroup);
                            }
                        }else{
                            sportMap.put(currentSport, new HashMap<>());
                            sportMap.get(currentSport).put(disciplineTitle, new ArrayList<>());
                            sportMap.get(currentSport).get(disciplineTitle).add(currentGroup);
                        }
                    }
                }
            }

            if(!disciplineTitle.equals("")){
                if(sportMap.get(currentSport) != null){
                    if(sportMap.get(disciplineTitle) != null){
                        sportMap.get(currentSport).get(disciplineTitle).add(currentGroup);
                    }
                }
            }
        }

        int m = 0;
        for(String sportName : sportMap.keySet()){
            m++;
            System.out.println("--------------START-------------");
            System.out.println("SPORT: " + sportName);
            for(String disciplineName : sportMap.get(sportName).keySet()){
                System.out.println("DISCIPLINE: " + disciplineName);
                for(String groupName : sportMap.get(sportName).get(disciplineName)){
                    TypeOfSport sport = sportRepository.findByTitle(sportName);
                    List<Discipline> disciplines = disciplineRepository.findAllByIdIn(sport.getDisciplineIds());
                    Optional<Discipline> disciplineOptional = disciplines.stream().filter(d -> d.getTitle().equals(disciplineName)).findAny();
                    if(disciplineOptional.isPresent()){
                        Discipline discipline = disciplineOptional.get();
                        AgeGroup ageGroup = new AgeGroup();
                        ageGroup.setTitle(groupName);
                        ageGroup.setDisciplineId(discipline.getId());
                        ageGroup = groupRepository.save(ageGroup);
                        discipline.addAgeGroup(ageGroup);
                        disciplineRepository.save(discipline);
                    }
                    System.out.println("GROUP: " + groupName);
                }
            }
            System.out.println("---------------" + m + "--------------");
        }
    }

    private void addParticipant(SubjectRepository subjectRepository, ParticipantRepository participantRepository, Iterator<Row> rowIter){
        while (rowIter.hasNext()){
            Row row = rowIter.next();
            String sportTitle = getFirstCharInAppearCase(row.getCell(0).toString());
            String ageGroupTitle = getFirstCharInAppearCase(row.getCell(1).toString());
            String disciplineName = row.getCell(2).toString();
            String subjectName = row.getCell(3).toString();
            String dateString = row.getCell(5).toString();

            Cell lastname = row.getCell(6);
            Cell name = row.getCell(7);
            Cell middleName = row.getCell(8);

            String ln = "";
            String n = "";
            String mn = "";

            if(lastname != null){
                if(!lastname.toString().equals("")){
                    ln = getFirstCharInAppearCase(lastname.toString());
                }
            }
            if(name != null){
                if(!name.toString().equals("")){
                    n = getFirstCharInAppearCase(name.toString());
                }
            }
            if(middleName != null){
                if(!middleName.toString().equals("")){
                    mn = getFirstCharInAppearCase(middleName.toString());
                }
            }

            LocalDate birthday = parseLocalDate(dateString);
            if(birthday != null){
                if(!ln.equals("") && !n.equals("")){
                    Subject subject = subjectRepository.findByTitle(subjectName);
                    Participant participant = new Participant();
                    participant.setBirthday(birthday);
                    participant.setLastname(ln);
                    participant.setName(n);
                    participant.setMiddleName(mn);
                    if(subject != null){
                        participant.addSubject(subject);
                    }
                    Participant test = participantRepository.findByLastnameAndNameAndBirthday(ln,n,birthday);
                    if(test == null) {
                        participant = participantRepository.save(participant);
                        if (subject != null) {
                            subject.addParticipant(participant);
                            subject = subjectRepository.save(subject);
                            System.out.println(participant.getLastname() + " " + subject.getTitle());
                        } else {
                            System.out.println(participant.getLastname());
                        }
                    }else{
                        System.out.println(test.getLastname() + " " + test.getName() + " in db");
                    }
                }
            }
        }
    }

    private void createSubjectAndAddInSport(SubjectRepository subjectRepository, TypeOfSportRepository sportRepository){
        HSSFWorkbook wb = getWorkBookFromHSSF("./src/main/resources/static/file/basic_sports.xls");
        HSSFSheet sheet = wb.getSheet("ВСЕГО");
        Iterator<Row> rowIter = sheet.rowIterator();
        Map<String, List<String>> map = new HashMap<>();
        String current = "";
        System.out.println("start process");
        while (rowIter.hasNext()){
            Row row = rowIter.next();
            Cell federalDistrict = row.getCell(0); // Федеральное деление
            Cell subjectName = row.getCell(1); // Название субъекта
            Cell sportName = row.getCell(2); // Названия спорта который является базовым для субъекта
            Cell sportType = row.getCell(3); // Олимп, Неолимп, Адапт
            Cell season = row.getCell(4); // Сезон
            if(row.getRowNum() != 0){
                if(!current.equals(subjectName.toString())) {
                    Subject subject = new Subject();
                    subject.setTitle(subjectName.toString());
                    subject.setFederalDistrict(parseDistrict(federalDistrict.toString()));
                    TypeOfSport sport = sportRepository.findByTitle(sportName.toString());
                    if(sport != null){
                        subject.addTypeOfSport(sport);
                    }
                    Subject saveSubject = subjectRepository.save(subject);
                    System.out.println(saveSubject.getTitle());
                    if(sport != null) {
                        sport.addSubject(saveSubject);
                        TypeOfSport saveSport = sportRepository.save(sport);
                        System.out.println(saveSport.getTitle());
                    }
                }else{
                    Subject foundSubject = subjectRepository.findByTitle(subjectName.toString());
                    if(foundSubject != null){
                        TypeOfSport foundSport = sportRepository.findByTitle(sportName.toString());
                        if(foundSport != null){
                            foundSport.addSubject(foundSubject);
                            foundSubject.addTypeOfSport(foundSport);
                            TypeOfSport savedSport = sportRepository.save(foundSport);
                            Subject savedSubject = subjectRepository.save(foundSubject);
                            System.out.println(savedSubject.getTitle() + " | " + savedSport.getTitle());
                        }
                    }
                }
            }
            current = subjectName.toString();
        }

        /*for(String subjectName : map.keySet()){
            System.out.println(subjectName + " | " + map.get(subjectName).size());
        }*/

        System.out.println("end process");
        try {
            wb.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addSeasonsAndFiltersInSport(TypeOfSportRepository sportRepository){
        HSSFWorkbook wb = getWorkBookFromHSSF("./src/main/resources/static/file/basic_sports.xls");
        HSSFSheet sheet = wb.getSheet("ВСЕГО");
        Iterator<Row> rowIter = sheet.rowIterator();
        System.out.println("start process");
        while (rowIter.hasNext()){
            Row row = rowIter.next();
            Cell federalDistrict = row.getCell(0); // Федеральное деление
            Cell subjectName = row.getCell(1); // Название субъекта
            Cell sportName = row.getCell(2); // Названия спорта который является базовым для субъекта
            Cell sportType = row.getCell(3); // Олимп, Неолимп, Адапт
            Cell season = row.getCell(4); // Сезон
            if(row.getRowNum() != 0){
                TypeOfSport sport = sportRepository.findByTitle(sportName.toString());
                if(sport != null) {
                    sport.setSeason(parseSeason(season.toString()));
                    sport.setSportFilterType(parseFilter(sportType.toString()));
                    TypeOfSport save = sportRepository.save(sport);
                    System.out.println("Найден: " + save.getTitle() + " | " + save.getSeason().getTitle() + " | " + save.getSportFilterType().getTitle());
                }else{
                    TypeOfSport s = new TypeOfSport();
                    s.setTitle(sportName.toString());
                    s.setSeason(parseSeason(season.toString()));
                    s.setSportFilterType(parseFilter(sportType.toString()));
                    TypeOfSport save = sportRepository.save(s);
                    System.out.println("Сохранен " + save.getTitle());
                }
            }
        }
        System.out.println("end process");
        try {
            wb.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addSportAndDiscipline(TypeOfSportRepository sportRepository, DisciplineRepository disciplineRepository){
        XSSFWorkbook wb = getWorkBookFromXSSF("./src/main/resources/static/file/test.xlsx");
        XSSFSheet sheet = wb.getSheet("Лист1");
        Iterator<Row> rowIter = sheet.rowIterator();
        Map<String, List<String>> map = new HashMap<>();
        String current = "";
        while (rowIter.hasNext()){
            Row row = rowIter.next();
            Cell counter = row.getCell(0);
            Cell sportName = row.getCell(1);
            Cell discipline = row.getCell(2);
            if(counter.toString().equals("n")){
                System.out.println("start process");
            }else if(!counter.toString().equals("")){
                current = sportName.toString();
                map.put(current,new ArrayList<>());
                map.get(current).add(discipline.toString());
            }else{
                map.get(current).add(discipline.toString());
            }
        }

        for(String sportName : map.keySet()){
            TypeOfSport typeOfSport = new TypeOfSport();
            typeOfSport.setTitle(sportName);
            TypeOfSport tSave = sportRepository.save(typeOfSport);
            for(String disciplineName : map.get(sportName)){
                Discipline discipline = new Discipline();
                discipline.setTitle(disciplineName);
                discipline.setTypeOfSportId(tSave.getId());
                Discipline dSave = disciplineRepository.save(discipline);
                tSave.addDiscipline(dSave);
                sportRepository.save(tSave);
                System.out.println(tSave.getTitle() + " | " + dSave.getTitle());
            }
        }

        System.out.println("end process");

        try {
            wb.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private XSSFWorkbook getWorkBookFromXSSF(String filePath){
        try{
            return new XSSFWorkbook(new FileInputStream(filePath));
        }catch (Exception e){
            System.out.println(e.getMessage());
            return null;
        }
    }

    private HSSFWorkbook getWorkBookFromHSSF(String filePath){
        try{
            return new HSSFWorkbook(new FileInputStream(filePath));
        }catch (Exception e){
            System.out.println(e.getMessage());
            return null;
        }
    }

    private String getFirstCharInAppearCase(String word){
        String result = "";
        if(!word.equals("")) {
            result = word.substring(0, 1).toUpperCase() + word.substring(1);
        }
        return result;
    }

    private LocalDate parseLocalDate(String dateString){

        // Это добавляет еще 8к записей ... но без дня и месяца
        /*if (Pattern.matches("^\\d{4}( г\\.)?$", dateString) || Pattern.matches("^\\d{4}(\\.0)?$", dateString)) {
            dateString = "01.01." + dateString.substring(0,4);
        }*/

        DateTimeFormatter formatter1  = DateTimeFormatter.ofPattern("dd.MM.yyyy г.");
        DateTimeFormatter formatter2  = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        DateTimeFormatter formatter3  = DateTimeFormatter.ofPattern("dd-MMM-yyyy", new Locale("ru"));

        LocalDate date1 = null;
        LocalDate date2 = null;
        LocalDate date3 = null;

        try {
            date1 = LocalDate.parse(dateString,formatter1);
        }catch (Exception e){
            //System.out.println(e);
        }
        try {
            date2 = LocalDate.parse(dateString,formatter2);
        }catch (Exception e){
            //System.out.println(e);
        }
        try {
            date3 = LocalDate.parse(dateString,formatter3);
        }catch (Exception e){
            //System.out.println(e);
        }

        if(date1 != null) {
            return date1;
        }else if (date2 != null){
            return date2;
        }else if (date3 != null){
            return date3;
        }else{
            return null;
        }
    }

    private Category parseCategory(String categoryTitle){
        Category category = Category.BR;

        if(categoryTitle.contains("1")){
            if(categoryTitle.contains("р") || categoryTitle.contains("Р")){
                if(!categoryTitle.contains("ю")) {
                    if(!categoryTitle.contains("\n") && categoryTitle.length() < 9) {
                        category = Category.R1;
                    }
                }else{
                    if(!categoryTitle.contains("\n") && !categoryTitle.contains("   ")) {
                        category = Category.YN1;
                    }
                }
            }
        }

        if(categoryTitle.contains("2")){
            if(categoryTitle.contains("р") || categoryTitle.contains("Р")){
                if(!categoryTitle.contains("ю")) {
                    if(!categoryTitle.contains("\n") && categoryTitle.length() < 9) {
                        category = Category.R2;
                    }
                }else{
                    if(!categoryTitle.contains("\n") && !categoryTitle.contains("   ")) {
                        category = Category.YN2;
                    }
                }
            }
        }

        if(categoryTitle.contains("3")){
            if(categoryTitle.contains("р") || categoryTitle.contains("Р")){
                if(!categoryTitle.contains("ю")) {
                    if(!categoryTitle.contains("\n") && categoryTitle.length() < 9) {
                        category = Category.R3;
                    }
                }else{
                    if(!categoryTitle.contains("\n") && !categoryTitle.contains("   ")) {
                        category = Category.YN3;
                    }
                }
            }
        }

        if(categoryTitle.equals("КМС")){
            category = Category.KMS;
        }

        if(categoryTitle.equals("МС")){
            category = Category.MS;
        }

        if(categoryTitle.equals("ЗМС")){
            category = Category.ZMS;
        }

        if(categoryTitle.equals("МСМК")){
            category = Category.MSMK;
        }

        return category;
    }

    private Season parseSeason(String season){
        return switch (season) {
            case "Летний" -> Season.SUMMER;
            case "Зимний" -> Season.WINTER;
            default -> Season.ALL;
        };
    }

    private SportFilterType parseFilter(String filer){
        return switch (filer) {
            case "олимпийский" -> SportFilterType.OLYMPIC;
            case "неолимпийский" -> SportFilterType.NO_OLYMPIC;
            default -> SportFilterType.ADAPTIVE;
        };
    }

    private FederalDistrict parseDistrict(String district){
        return switch (district) {
            case "ЦФО" -> FederalDistrict.CFO;
            case "СЗФО" -> FederalDistrict.SZFO;
            case "ЮФО" -> FederalDistrict.YFO;
            case "СКФО" -> FederalDistrict.SKFO;
            case "ПФО" -> FederalDistrict.PFO;
            case "УФО" -> FederalDistrict.UFO;
            case "СФО" -> FederalDistrict.SFO;
            case "ДФО" -> FederalDistrict.DFO;
            default -> null;
        };
    }
}
