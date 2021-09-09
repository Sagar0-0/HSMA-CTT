package de.hs_mannheim.informatik.ct.persistence.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import de.hs_mannheim.informatik.ct.controller.RoomController;
import de.hs_mannheim.informatik.ct.util.RoomTypeConverter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.stereotype.Service;

/*
 * Corona Tracking Tool der Hochschule Mannheim
 * Copyright (C) 2021 Hochschule Mannheim
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

import de.hs_mannheim.informatik.ct.model.Room;
import de.hs_mannheim.informatik.ct.persistence.repositories.RoomRepository;
import lombok.NonNull;

@Service
public class RoomService {


    private final String COMMA_DELIMITER = ";";

    @Autowired
    private RoomRepository roomsRepo;

    public Optional<Room> findByName(String roomName) {
        try {
            return roomsRepo.findByNameIgnoreCase(roomName);
        } catch (IncorrectResultSizeDataAccessException e) {
            return roomsRepo.findById(roomName);
        }
    }

    /**
     * Gets a room by id or throws a RoomNotFoundException.
     *
     * @param roomId The rooms id.
     * @return The room.
     */
    public Room getRoomOrThrow(String roomId) {
        Optional<Room> room = this.findByName(roomId);
        if (room.isPresent()) {
            return room.get();
        } else {
            throw new RoomController.RoomNotFoundException();
        }
    }

    public Room saveRoom(@NonNull Room room) {
            return saveAllRooms(Collections.singletonList(room)).get(0);
    }

    public List<Room> saveAllRooms(@NonNull List<Room> roomList) {
        return roomsRepo.saveAll(checkRoomPin(roomList));
    }

    private List<Room> checkRoomPin(List<Room> roomList) {
        List<Room> oldRoomList = roomsRepo.findAll();
        for (Room r : roomList) {
            checkRoomPinFormatForRoom(r);
            for (Room rOld : oldRoomList) {
                if (r.getName().equals(rOld.getName())) {
                    r.setRoomPin(rOld.getRoomPin());
                    break;
                }
            }
        }
        return roomList;
    }

    private void checkRoomPinFormatForRoom(Room room) {
        try{
            Long.parseLong(room.getRoomPin());
        } catch (NumberFormatException err) {
            throw new IllegalArgumentException("Wrong roomPin format 'not a number' so can't proceed.");
        } catch (NullPointerException err) {
            throw new IllegalArgumentException("Can't save a Room with a null roomPin.");
        }

    }

    // TODO: Maybe check if csv is correctly formatted (or accept that the user
    // uploads only correct files?)
    public void importFromCsv(BufferedReader csv) {
        csv.lines().map((line) -> {
            String[] values = line.split(COMMA_DELIMITER);
            String building = values[0];
            String roomName = values[1];
            int roomCapacity = Integer.parseInt(values[2]);
            int rna = Integer.parseInt(values[3]);
            return new Room(roomName, building, roomCapacity, RoomTypeConverter.getInstance().convertRNAToRoomType(rna));
        }).forEach(this::saveRoom);
    }

    public void importFromExcel(InputStream is) throws IOException {
        XSSFWorkbook workbook = XSSFWorkbookFactory.createWorkbook(is);
        Sheet sheet = workbook.getSheetAt(0);

        List<Room> rooms = new LinkedList<>();

        Iterator<Row> iter = sheet.rowIterator();
        if (iter.hasNext())
            iter.next(); // skip first line

        while (iter.hasNext()) {
            Row row = iter.next();

            String building, roomName;
            int rna;
            int roomCapacity;

            try {
                rna = (int) row.getCell(8).getNumericCellValue();  // rna = name of the column in the import file, number of the room type
                building = row.getCell(32).getStringCellValue();
                roomName = row.getCell(34).getStringCellValue();
                roomCapacity = (int) row.getCell(35).getNumericCellValue();

                // Sometimes POI returns a partially empty row, might be a bug in POI or just weird excel things.
                if (building.equals("") || roomName.equals("")) {
                    continue;
                }
            } catch (NullPointerException e) {

                // Sometimes POI returns a partially empty row, might be a bug in POI or just weird excel things.
                continue;
            }
            RoomTypeConverter.RoomType type = RoomTypeConverter.getInstance().convertRNAToRoomType(Integer.valueOf(rna));
            rooms.add(new Room(roomName, building, roomCapacity, type));
        }

        this.saveAllRooms(rooms);

        workbook.close();
    }
}
