import hlt.*;
import java.util.*;

import java.util.ArrayList;

public class MyBot {

    public static void main(final String[] args) {
        final Networking networking = new Networking();
        final GameMap gameMap = networking.initialize("BetterBot");
        Random rand = new Random();
        final ArrayList<Move> moveList = new ArrayList<>();
        int lastresortshipID = -1;
        Position lastresortshiptarget = null;
        double initx = -1;
        double inity = -1;
        int turnnum = 0;
        for (;;) {
            boolean nolastresort = true;
            turnnum++;
            moveList.clear();
            gameMap.updateMap(Networking.readLineIntoMetadata());
            try{
            int shipcount = 0;
          for (Planet p:gameMap.getAllPlanets().values() ){p.docking=0;p.issaved = false;}
         if(turnnum==1){
              int count = 0;
              Ship[] ships = gameMap.getMyPlayer().getShips().values().toArray(new Ship[0]);
              initx = ships[0].getXPos();
              inity = ships[0].getYPos();
              sortClosestPosition(ships, 0.1, 0.1);
              for (final Ship ship : gameMap.getMyPlayer().getShips().values()){moveList.add(new ThrustMove(ship,count*90-90,Constants.MAX_SPEED));count++;}
            }
            
          else{
            for (Planet p:gameMap.getAllPlanets().values())
            {
              if(p.getOwner()!=gameMap.getMyPlayerId()){continue;}
              Ship enemyship = issafe(gameMap, p);
              if(enemyship!=null)
              {
                Ship[] ships = gameMap.getMyPlayer().getShips().values().toArray(new Ship[0]);
                sortClosestPosition(ships, enemyship.getXPos(),enemyship.getYPos());
                boolean istargetted = false;
                for(Ship s: ships)
                {
                  if(s.target==enemyship){istargetted = true; break;}
                }
                if(istargetted){continue;}
                int index = 0;
                while(index<ships.length&&(ships[index].getDockingStatus()!=Ship.DockingStatus.Undocked||ships[index].target!=null))
                {
                  index++;
                  continue;
                }
                if(index<ships.length){ships[index].target = enemyship;}
              }
            }
            try{if(lastresortshipID>=0){gameMap.getMyPlayer().getShips().get(lastresortshipID).target = lastresortshiptarget;}}catch(Exception e){}
            for (final Ship ship : gameMap.getMyPlayer().getShips().values()) {
                
                if (ship.getDockingStatus() != Ship.DockingStatus.Undocked) {
                    continue;
                }
                try{
                if (gameMap.getAllPlayers().size()>2&&nolastresort&&turnnum%40==0&&turnnum>40&&ship.target==null)
                {
                    double magicnum = turnnum/50;
                    if(initx>gameMap.getWidth()/2&&inity>gameMap.getHeight()/2)ship.target = new Position(gameMap.getWidth()-magicnum, gameMap.getHeight()-magicnum);
                    else if(initx<=gameMap.getWidth()/2&&inity>gameMap.getHeight()/2)ship.target = new Position(magicnum, gameMap.getHeight()-magicnum);
                    else if(initx<=gameMap.getWidth()/2&&inity<=gameMap.getHeight()/2)ship.target = new Position(magicnum, magicnum);
                    else ship.target = new Position(gameMap.getWidth()-magicnum, magicnum);
                    nolastresort=false;
                    lastresortshipID = ship.getId();
                    lastresortshiptarget = ship.target;
                }
                if (ship.target!=null){ThrustMove t = Navigation.navigateShipTowardsTarget(gameMap, ship, ship.target,Constants.MAX_SPEED,true, Constants.MAX_NAVIGATION_CORRECTIONS, Math.PI/180.0 );
                  if(t!=null){moveList.add(t);continue;}}}catch(Exception e){}
                if (gameMap.getAllPlayers().size()>2&&lastresortshiptarget!=null&&ship.getDistanceTo(lastresortshiptarget)<6)continue;
                boolean moved = false;
                Planet[] p = gameMap.getAllPlanets().values().toArray(new Planet[0]);
                sortClosestPosition(p,ship.getXPos(), ship.getYPos());
                Planet someEnemyPlanet = null;
                double closestdist = p[0].getDistanceTo(p[p.length-1])/3;
                for (Planet planet : p) {
                    Ship enemyship = null;
                    if (planet.isOwned()) {
                      if(planet.getOwner()==gameMap.getMyPlayerId()){
                          if(((planet.isFull())||ship.getDistanceTo(planet)>closestdist))continue;}
                      else {if(someEnemyPlanet==null){someEnemyPlanet=planet;}continue;}
                    }
                    else if(ship.getDistanceTo(planet)>closestdist*2){continue;}
                    if (planet.getDockingSpots()-planet.getDockedShips().size()>planet.docking){
                    if (ship.canDock(planet)) {
                        moved = true;
                        moveList.add(new DockMove(ship, planet));
                        planet.docking++;
                        break;
                    }

                    final ThrustMove newThrustMove = Navigation.navigateShipToDock(gameMap, ship, planet, Constants.MAX_SPEED);
                    if (newThrustMove != null) {
                        moved = true;
                        moveList.add(newThrustMove);
                    }
                    break;
                  }
                }
                
                if((!moved)&&(someEnemyPlanet!=null)){ThrustMove t = Navigation.navigateShipTowardsTarget(gameMap, ship, gameMap.getShip(someEnemyPlanet.getOwner(),someEnemyPlanet.getDockedShips().get(0)),Constants.MAX_SPEED,true, Constants.MAX_NAVIGATION_CORRECTIONS, Math.PI/180.0 );
                  if(t!=null){moveList.add(t);}else{t=Navigation.navigateShipTowardsTarget(gameMap, ship, p[p.length-1],Constants.MAX_SPEED,true, Constants.MAX_NAVIGATION_CORRECTIONS, Math.PI/180.0 );}
                }
                shipcount++;
            }}}catch(Exception e){}
            Networking.sendMoves(moveList);
        }
    }
    public static Ship issafe(GameMap gameMap, Planet p)
    {
      if(p.issaved){return null;}
      for(Ship ship: gameMap.getAllShips())
      {
        if(p.getDistanceTo(ship)<10+p.getRadius())
        {
          if(ship.getOwner()!=gameMap.getMyPlayerId())return ship;
        }
      }
      return null;
    }
    public static void sortClosestPosition(Position[] p, double x, double y)
    {
      Planetair[] pl = new Planetair[p.length];
      for(int i = 0; i < p.length; i++)
      {
        pl[i]=new Planetair();
        pl[i].x=x;
        pl[i].y=y;
        pl[i].p=p[i];
      }
      Arrays.sort(pl);
      for(int i = 0; i < p.length; i++)
      {
        p[i]=pl[i].p;
      }
    }
}

class Planetair implements Comparable<Planetair>
{
  public Position p;
  public double x;
  public double y;
  public int compareTo(Planetair pp)
  {
    double xx = p.getXPos();
    double yy = p.getYPos();
    double f = Math.hypot(xx-x, yy-y);
    xx = pp.p.getXPos();
    yy = pp.p.getYPos();
    double ff = Math.hypot(xx-pp.x, yy-pp.y);
    double fff = f - ff;
    if(fff>0)return 1;
    else return -1;
  }
}