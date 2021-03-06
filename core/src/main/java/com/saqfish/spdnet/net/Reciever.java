package com.saqfish.spdnet.net;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saqfish.spdnet.net.actor.Player;
import com.saqfish.spdnet.net.events.Events;
import com.saqfish.spdnet.net.events.Receive;
import com.saqfish.spdnet.net.events.Send;
import com.saqfish.spdnet.net.windows.NetWindow;
import com.saqfish.spdnet.utils.GLog;
import com.watabou.utils.DeviceCompat;

import io.socket.emitter.Emitter;

public class Reciever {
        private ObjectMapper mapper;
        private Net net;

        public Reciever(Net net, ObjectMapper mapper){
                this.net = net;
                this.mapper = mapper;
        }

        public void startAll(){
                Emitter.Listener onMessage = args -> {
                        String data = (String) args[0];
                        handleMessage(data);
                };
                Emitter.Listener onAction = args -> {
                        int type = (int) args[0];
                        String data = (String) args[1];
                        handleAction(type, data);
                };
                Emitter.Listener onMotd = args -> {
                        String data = (String) args[0];
                        handleMotd(data);
                };
                net.socket().on(Events.ACTION, onAction);
                net.socket().on(Events.MESSAGE, onMessage);
                net.socket().once(Events.MOTD, onMotd);
        }

        public void cancelAll(){
                net.socket().off(Events.ACTION);
                net.socket().off(Events.MESSAGE);
                net.socket().off(Events.MOTD);
        }

        public void handleMessage(String json){
                try{
                        Send.Message message = mapper.readValue(json, Send.Message.class);
                        NetWindow.message(message.data);
                } catch (JsonProcessingException e) {
                        e.printStackTrace();
                }
        }

        public void handleAction(int type, String json) {
                Player player;
                Receive.Join join;
                try {
                        switch (type){
                                case Receive.MOVE:
                                        Receive.Move m = mapper.readValue(json, Receive.Move.class);
                                        Player.movePlayer(Player.getPlayer(m.id), m.pos, m.playerClass);
                                        break;
                                case Receive.JOIN:
                                        join = mapper.readValue(json, Receive.Join.class);
                                        Player.addPlayer(join.id, join.nick, join.playerClass, join.pos, join.depth, join.items);
                                        break;
                                case Receive.JOIN_LIST:
                                        Receive.JoinList jl = mapper.readValue(json, Receive.JoinList.class);
                                        for (int i = 0; i < jl.players.length; i++) {
                                                Receive.Join j = jl.players[i];
                                                Player.addPlayer(j.id, j.nick, j.playerClass, j.pos, j.depth, j.items);
                                        }
                                        break;
                                case Receive.LEAVE:
                                        Receive.Leave l = mapper.readValue(json, Receive.Leave.class);
                                        player = Player.getPlayer(l.id);
                                        if(player != null) player.leave();
                                        break;
                                case Receive.DEATH:
                                        Receive.Death d = mapper.readValue(json, Receive.Death.class);
                                        GLog.n(d.nick+" "+d.cause);
                                        GLog.newLine();
                                        break;
                                default:
                                        DeviceCompat.log("Unknown Action",json);
                        }
                } catch (JsonProcessingException e) {
                        e.printStackTrace();
                }
        }

        public void handleMotd(String json){
                try{
                        Receive.Motd motd = mapper.readValue(json, Receive.Motd.class);
                        NetWindow.motd(motd.motd, motd.seed);
                        net.seed(motd.seed);
                } catch (JsonProcessingException e) {
                        e.printStackTrace();
                }
        }
}
