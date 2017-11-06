import hlt.*;
import java.util.*;

import java.util.ArrayList;

public class MyBot2 {

    public static void main(final String[] args) {
        final Networking networking = new Networking();
        final GameMap gameMap = networking.initialize("Tamagocchi");
        Random rand = new Random();
        final ArrayList<Move> moveList = new ArrayList<>();
        for (;;) {
            moveList.clear();
            gameMap.updateMap(Networking.readLineIntoMetadata());

          for (final Ship ship : gameMap.getMyPlayer().getShips().values()) {
                if (ship.getDockingStatus() != Ship.DockingStatus.Undocked) {
                    continue;
                }
                boolean moved = false;
                Planet[] p = gameMap.getAllPlanets().values().toArray(new Planet[0]);
                //sortClosestPlanet(p,ship.getXPos(), ship.getYPos());
                Planet someEnemyPlanet = null;
                for (final Planet planet : p) {
                    if (planet.isOwned()) {
                      if(planet.getOwner()==gameMap.getMyPlayerId()){
                          if((planet.isFull()))continue;}
                      else {if(someEnemyPlanet==null){someEnemyPlanet=planet;}continue;}
                    }

                    if (ship.canDock(planet)) {
                        moved = true;
                        moveList.add(new DockMove(ship, planet));
                        break;
                    }

                    final ThrustMove newThrustMove = Navigation.navigateShipToDock(gameMap, ship, planet, Constants.MAX_SPEED);
                    if (newThrustMove != null) {
                        moved = true;
                        moveList.add(newThrustMove);
                    }
                    
                    break;
                }
                
                if((!moved)&&(someEnemyPlanet!=null)){ThrustMove t = Navigation.navigateShipTowardsTarget(gameMap, ship, gameMap.getShip(someEnemyPlanet.getOwner(),someEnemyPlanet.getDockedShips().get(0)),Constants.MAX_SPEED,true, Constants.MAX_NAVIGATION_CORRECTIONS, Math.PI/180.0 );
                  if(t!=null){moveList.add(t);}
                }
                
            }
            Networking.sendMoves(moveList);
        }
    }
}

