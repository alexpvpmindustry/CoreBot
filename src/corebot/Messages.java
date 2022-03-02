package corebot;

import arc.files.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.CommandHandler.*;
import arc.util.io.Streams;
import arc.util.serialization.*;
import arc.util.serialization.Jval.*;
import mindustry.*;
import mindustry.game.*;
import mindustry.type.*;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.Message.*;
import net.dv8tion.jda.api.events.guild.member.*;
import net.dv8tion.jda.api.events.message.*;
import net.dv8tion.jda.api.events.message.guild.*;
import net.dv8tion.jda.api.hooks.*;
import net.dv8tion.jda.api.requests.*;
import net.dv8tion.jda.api.utils.*;
import net.dv8tion.jda.api.utils.cache.*;
import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

import javax.imageio.*;
import java.awt.image.*;
import java.io.*;
import java.text.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import java.util.stream.*;
import java.util.zip.*;

import static corebot.CoreBot.*;

public class Messages extends ListenerAdapter{
    private static final String prefix = "!";
    private static final int scamAutobanLimit = 3, pingSpamLimit = 10;
    private static final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
    private static final String[] warningStrings = {"once", "twice", "thrice", "too many times"};

    // https://stackoverflow.com/a/48769624
    private static final Pattern urlPattern = Pattern.compile("(?:(?:https?):\\/\\/)?[\\w/\\-?=%.]+\\.[\\w/\\-&?=%.]+");
    private static final Set<String> trustedDomains = Set.of(
        "discord.com",
        "discord.co",
        "discord.gg",
        "discord.media",
        "discord.gift",
        "discordapp.com",
        "discordapp.net",
        "discordstatus.com"
    );

    private static final Pattern invitePattern = Pattern.compile("(discord\\.gg/\\w|discordapp\\.com/invite/\\w|discord\\.com/invite/\\w)");
    private static final Pattern linkPattern = Pattern.compile("http(s?)://");
    private static final Pattern notScamPattern = Pattern.compile("discord\\.py|discord\\.js");
    private static final Pattern scamPattern = Pattern.compile(String.join("|",
        "stea.*co.*\\.ru",
        "http.*stea.*c.*\\..*trad",
        "csgo.*kni[fv]e",
        "cs.?go.*inventory",
        "cs.?go.*cheat",
        "cheat.*cs.?go",
        "cs.?go.*skins",
        "skins.*cs.?go",
        "stea.*com.*partner",
        "steamcommutiny",
        "di.*\\.gift.*nitro",
        "http.*disc.*gift.*\\.",
        "free.*nitro.*http",
        "http.*free.*nitro.*",
        "nitro.*free.*http",
        "discord.*nitro.*free",
        "free.*discord.*nitro",
        "@everyone.*http",
        "http.*@everyone",
        "discordgivenitro",
        "http.*gift.*nitro",
        "http.*nitro.*gift",
        "http.*n.*gift",
        "nitro.*http.*disc.*nitro",
        "http.*click.*nitro",
        "http.*st.*nitro",
        "http.*nitro",
        "stea.*give.*nitro",
        "discord.*nitro.*steam.*get",
        "gift.*nitro.*http",
        "http.*discord.*gift",
        "discord.*nitro.*http",
        "personalize.*your*profile.*http",
        "nitro.*steam.*http",
        "steam.*nitro.*http",
        "nitro.*http.*d",
        "http.*d.*gift",
        "gift.*http.*d.*s",
        "discord.*steam.*http.*d",
        "nitro.*steam.*http",
        "steam.*nitro.*http",
        "dliscord.com",
        "free.*nitro.*http",
        "discord.*nitro.*http",
        "@everyone.*http",
        "http.*@everyone",
        "@everyone.*nitro",
        "nitro.*@everyone",
        "discord.*gi.*nitro"
    ));

    private final ObjectMap<String, UserData> userData = new ObjectMap<>();
    private final CommandHandler handler = new CommandHandler(prefix);
    private final CommandHandler adminHandler = new CommandHandler(prefix);
    private final JDA jda;

    public Guild guild;
    public TextChannel
    pluginChannel, crashReportChannel, announcementsChannel, artChannel,
    mapsChannel, moderationChannel, schematicsChannel, baseSchematicsChannel,
    logChannel, joinChannel, videosChannel, streamsChannel, testingChannel,
    alertsChannel, curatedSchematicsChannel;
    LongSeq schematicChannels = new LongSeq();
    Seq<CommandHandler> handlers = new Seq<>();
    Seq<String> handlerstrings = new Seq<>();
    public Long mapsChannelIDLong,schematicsChannelIDLong,guildIDLong;
    public Messages(){
        String token = prefs2.get("botToken","");
        if (token.length()==0){
            prefs2.put("botToken","input_bot_token");
            prefs2.put("mapsChannelIDLong","input_mapsChannelIDLong");
            prefs2.put("schematicsChannelIDLong","input_schematicsChannelIDLong");
            prefs2.put("guildIDLong","input_guildIDLong");
        } else{
            mapsChannelIDLong = Long.parseLong(prefs2.get("mapsChannelIDLong",""));
            schematicsChannelIDLong = Long.parseLong(prefs2.get("schematicsChannelIDLong",""));
            guildIDLong = Long.parseLong(prefs2.get("guildIDLong",""));
        }
        register();
        try{
            jda = JDABuilder.createDefault(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_EMOJIS, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES)
                .setMemberCachePolicy(MemberCachePolicy.ALL).disableCache(CacheFlag.VOICE_STATE).build();
            jda.awaitReady();
            jda.addEventListener(this);
            
            loadChannels();

            Log.info("Discord bot up.");
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }
    
    TextChannel channel(long id){
        return guild.getTextChannelById(id);
    }
    
    void loadChannels(){

        //all guilds and channels are loaded here for faster lookup
        guild = jda.getGuildById(guildIDLong);
        //pluginChannel = channel(785543837488775218L);
        //crashReportChannel = channel(785543837488775218L);
        //announcementsChannel = channel(785543837488775218L);
        //artChannel = channel(785543837488775218L);
        mapsChannel = channel(mapsChannelIDLong);
        //moderationChannel = channel(785543837488775218L);
        schematicsChannel = channel(schematicsChannelIDLong);
        baseSchematicsChannel = channel(schematicsChannelIDLong);
        //logChannel = channel(785543837488775218L);
        //joinChannel = channel(785543837488775218L);
        //streamsChannel = channel(785543837488775218L);
        //videosChannel = channel(785543837488775218L);
        //testingChannel = channel(785543837488775218L);
        //alertsChannel = channel(785543837488775218L);
        curatedSchematicsChannel = channel(schematicsChannelIDLong);
        //Log.info(Objects.requireNonNull(guild.getTextChannelById(785543837488775218L)).getGuild().toString());
        schematicChannels.add(schematicsChannel.getIdLong(), baseSchematicsChannel.getIdLong(), curatedSchematicsChannel.getIdLong());
    }

    void register(){
        handler.<Message>register("help", "Displays all bot commands.", (args, msg) -> {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < handlers.size; i++) {
                CommandHandler hand = handlers.get(i);
                builder.append( handlerstrings.get(i) ).append("\n");
                for (Command command : hand.getCommandList()) {
                    builder.append(prefix);
                    builder.append("**");
                    builder.append(command.text);
                    builder.append("**");
                    if (command.params.length > 0) {
                        builder.append(" *");
                        builder.append(command.paramText);
                        builder.append("*");
                    }
                    builder.append(" - ");
                    builder.append(command.description);
                    builder.append("\n");
                }
            }
            info(msg.getChannel(), "Commands", builder.toString());
        });

        handler.<Message>register("ping", "<ip>", "Pings a server.", (args, msg) -> {
            net.pingServer(args[0], result -> {
                if(result.name != null){
                    info(msg.getChannel(), "Server Online", "Host: @\nPlayers: @\nMap: @\nWave: @\nVersion: @\nPing: @ms",
                    Strings.stripColors(result.name), result.players, Strings.stripColors(result.mapname), result.wave, result.version, result.ping);
                }else{
                    errDelete(msg, "Server Offline", "Timed out.");
                }
            });
        });

        handler.<Message>register("postmap", "Post a .msav file to the #map-submissions channel.", (args, msg) -> {

            if(msg.getAttachments().size() != 1 || !msg.getAttachments().get(0).getFileName().endsWith(".msav")){
                errDelete(msg, "You must have one .msav file in the same message as the command!");
                return;
            }

            Attachment a = msg.getAttachments().get(0);

            try{
                ContentHandler.Map map = contentHandler.readMap(net.download(a.getUrl()));
                new File("cache/").mkdir();
                File mapFile = new File("cache/" + a.getFileName());
                Fi imageFile = Fi.get("cache/image_" + a.getFileName().replace(".msav", ".png"));
                Streams.copy(net.download(a.getUrl()), new FileOutputStream(mapFile));
                ImageIO.write(map.image, "png", imageFile.file());

                EmbedBuilder builder = new EmbedBuilder().setColor(normalColor).setColor(normalColor)
                .setImage("attachment://" + imageFile.name())
                .setAuthor(msg.getAuthor().getName(), msg.getAuthor().getEffectiveAvatarUrl(), msg.getAuthor().getEffectiveAvatarUrl())
                .setTitle(map.name == null ? a.getFileName().replace(".msav", "") : map.name);

                if(map.description != null) builder.setFooter(map.description);

                mapsChannel.sendFile(mapFile).addFile(imageFile.file()).setEmbeds(builder.build()).queue();

                text(msg, "*Map posted successfully.*");
            }catch(Exception e){
                String err = Strings.neatError(e, true);
                int max = 900;
                errDelete(msg, "Error parsing map.", err.length() < max ? err : err.substring(0, max));
            }
        });
        handler.<Message>register("postscheme","Post a scheme file to the schematics channel",(args,msg)->{
            //schematic preview
            if(msg.getAttachments().size() == 1 && msg.getAttachments().get(0).getFileExtension() != null && msg.getAttachments().get(0).getFileExtension().equals(Vars.schematicExtension)){
                try{
                    Schematic schem = msg.getAttachments().size() == 1 ? contentHandler.parseSchematicURL(msg.getAttachments().get(0).getUrl()) : contentHandler.parseSchematic(msg.getContentRaw());
                    BufferedImage preview = contentHandler.previewSchematic(schem);
                    String sname = schem.name().replace("/", "_").replace(" ", "_");
                    if(sname.isEmpty()) sname = "empty";

                    new File("cache").mkdir();
                    File previewFile = new File("cache/img_" + UUID.randomUUID() + ".png");
                    File schemFile = new File("cache/" + sname + "." + Vars.schematicExtension);
                    Schematics.write(schem, new Fi(schemFile));
                    ImageIO.write(preview, "png", previewFile);

                    EmbedBuilder builder = new EmbedBuilder().setColor(normalColor).setColor(normalColor)
                            .setImage("attachment://" + previewFile.getName())
                            .setAuthor(msg.getAuthor().getName(), msg.getAuthor().getEffectiveAvatarUrl(), msg.getAuthor().getEffectiveAvatarUrl()).setTitle(schem.name());

                    if(!schem.description().isEmpty()) builder.setFooter(schem.description());

                    StringBuilder field = new StringBuilder();

                    for(ItemStack stack : schem.requirements()){
                        List<Emote> emotes = guild.getEmotesByName(stack.item.name.replace("-", ""), true);
                        Emote result = emotes.isEmpty() ? guild.getEmotesByName("ohno", true).get(0) : emotes.get(0);

                        field.append(result.getAsMention()).append(stack.amount).append("  ");
                    }
                    builder.addField("Requirements", field.toString(), false);

                    schematicsChannel.sendFile(schemFile).addFile(previewFile).setEmbeds(builder.build()).queue();
                    msg.delete().queue();
                }catch(Throwable e){
                    if(schematicChannels.contains(msg.getChannel().getIdLong())){
                        msg.delete().queue();
                        try{
                            msg.getAuthor().openPrivateChannel().complete().sendMessage("Invalid schematic: " + e.getClass().getSimpleName() + (e.getMessage() == null ? "" : " (" + e.getMessage() + ")")).queue();
                        }catch(Exception e2){
                            e2.printStackTrace();
                        }
                    }
                    //ignore errors
                }
            }else{
                errDelete(msg, "Error parsing schematic.");
            }
        });

        adminHandler.<Message>register("userinfo", "<@user>", "Get user info.", (args, msg) -> {
            String author = args[0].substring(2, args[0].length() - 1);
            if(author.startsWith("!")) author = author.substring(1);
            try{
                long l = Long.parseLong(author);
                User user = jda.retrieveUserById(l).complete();

                if(user == null){
                    errDelete(msg, "That user (ID @) is not in the cache. How did this happen?", l);
                }else{
                    Member member = guild.retrieveMember(user).complete();

                    info(msg.getChannel(), "Info for " + member.getEffectiveName(),
                        "Nickname: @\nUsername: @\nID: @\nStatus: @\nRoles: @\nIs Admin: @\nTime Joined: @",
                        member.getNickname(),
                        user.getName(),
                        member.getIdLong(),
                        member.getOnlineStatus(),
                        member.getRoles().stream().map(Role::getName).collect(Collectors.toList()),
                        isAdmin(user),
                        member.getTimeJoined()
                    );
                }
            }catch(Exception e){
                errDelete(msg, "Incorrect name format or missing user.");
            }
        });

        adminHandler.<Message>register("delete", "<amount>", "Delete some ", (args, msg) -> {
            try{
                int number = Integer.parseInt(args[0]);
                MessageHistory hist = msg.getChannel().getHistoryBefore(msg, number).complete();
                msg.delete().queue();
                msg.getTextChannel().deleteMessages(hist.getRetrievedHistory()).queue();
            }catch(NumberFormatException e){
                errDelete(msg, "Invalid number.");
            }
        });
        handlerstrings.add("**[**Public Commands**]**","**[**Admin Commands**]**");
        handlers.add(handler,adminHandler);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event){
        var msg = event.getMessage();

        if(msg.getAuthor().isBot() || msg.getChannel().getType() != ChannelType.TEXT) return;

        EmbedBuilder log = new EmbedBuilder()
        .setAuthor(msg.getAuthor().getName(), msg.getAuthor().getEffectiveAvatarUrl(), msg.getAuthor().getEffectiveAvatarUrl())
        .setDescription(msg.getContentRaw().length() >= 2040 ? msg.getContentRaw().substring(0, 2040) + "..." : msg.getContentRaw())
        .addField("Author", msg.getAuthor().getAsMention(), false)
        .addField("Channel", msg.getTextChannel().getAsMention(), false)
        .setColor(normalColor);

        String text = msg.getContentRaw();
        if(schematicChannels.contains(msg.getChannel().getIdLong()) && !isAdmin(msg.getAuthor())){
            //delete non-schematics
            msg.delete().queue();
            try{
                msg.getAuthor().openPrivateChannel().complete().sendMessage("Only send valid schematics in the #schematics channel. You may send them either as clipboard text or as a schematic file.").queue();
            }catch(Exception e){
                e.printStackTrace();
            }
            return;
        }

        if(!text.replace(prefix, "").trim().isEmpty()){
            if(isAdmin(msg.getAuthor())){
                boolean unknown = handleResponse(msg, adminHandler.handleMessage(text, msg), false);
                handleResponse(msg, handler.handleMessage(text, msg), !unknown);
            }else{
                handleResponse(msg, handler.handleMessage(text, msg), true);
            }
        }
    }

    @Override
    public void onGuildMessageUpdate(GuildMessageUpdateEvent event){
        var msg = event.getMessage();

        if(isAdmin(msg.getAuthor()) || checkSpam(msg, true)){
            return;
        }

        if((msg.getChannel().getIdLong() == artChannel.getIdLong()) && msg.getAttachments().isEmpty()){
            msg.delete().queue();
            try{
                msg.getAuthor().openPrivateChannel().complete().sendMessage("Don't send messages without images in that channel.").queue();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    public void text(MessageChannel channel, String text, Object... args){
        channel.sendMessage(Strings.format(text, args)).queue();
    }

    public void text(Message message, String text, Object... args){
        text(message.getChannel(), text, args);
    }

    public void info(MessageChannel channel, String title, String text, Object... args){
        channel.sendMessageEmbeds(new EmbedBuilder().addField(title, Strings.format(text, args), true).setColor(normalColor).build()).queue();
    }

    public void infoDesc(MessageChannel channel, String title, String text, Object... args){
        channel.sendMessageEmbeds(new EmbedBuilder().setTitle(title).setDescription(Strings.format(text, args)).setColor(normalColor).build()).queue();
    }

    /** Sends an error, deleting the base message and the error message after a delay. */
    public void errDelete(Message message, String text, Object... args){
        errDelete(message, "Error", text, args);
    }

    /** Sends an error, deleting the base message and the error message after a delay. */
    public void errDelete(Message message, String title, String text, Object... args){
        message.getChannel().sendMessageEmbeds(new EmbedBuilder()
        .addField(title, Strings.format(text, args), true).setColor(errorColor).build())
        .queue(result -> result.delete().queueAfter(messageDeleteTime, TimeUnit.MILLISECONDS));

        //delete base message too
        message.delete().queueAfter(messageDeleteTime, TimeUnit.MILLISECONDS);
    }

    private Seq<String> getWarnings(User user){
        var list = prefs.getArray("warning-list-" + user.getIdLong());
        //remove invalid warnings
        list.removeAll(s -> {
            String[] split = s.split(":::");
            return Duration.ofMillis((System.currentTimeMillis() - Long.parseLong(split[0]))).toDays() >= warnExpireDays;
        });

        return list;
    }

    private Jval fixJval(Jval val){
        if(val.isArray()){
            Seq<Jval> list = val.asArray().copy();
            for(Jval child : list){
                if(child.isObject() && (child.has("item")) && child.has("amount")){
                    val.asArray().remove(child);
                    val.asArray().add(Jval.valueOf(child.getString("item", child.getString("liquid", "")) + "/" + child.getInt("amount", 0)));
                }else{
                    fixJval(child);
                }
            }
        }else if(val.isObject()){
            Seq<String> keys = val.asObject().keys().toArray();

            for(String key : keys){
                Jval child = val.get(key);
                if(child.isObject() && (child.has("item")) && child.has("amount")){
                    val.remove(key);
                    val.add(key, Jval.valueOf(child.getString("item", child.getString("liquid", "")) + "/" + child.getInt("amount", 0)));
                }else{
                    fixJval(child);
                }
            }
        }

        return val;
    }

    boolean isAdmin(User user){
        var member = guild.retrieveMember(user).complete();
        return member != null && member.getRoles().stream().anyMatch(role -> role.getName().equals("Admin (Discord)") || role.getName().equals("Admin (Mindustry)") );
    }

    boolean checkSpam(Message message, boolean edit){

        if(message.getChannel().getType() != ChannelType.PRIVATE){
            Seq<String> mentioned =
                //ignore reply messages, bots don't use those
                message.getReferencedMessage() != null ? new Seq<>() :
                //get all mentioned members and roles in one list
                Seq.with(message.getMentionedMembers()).map(IMentionable::getAsMention).and(Seq.with(message.getMentionedRoles()).map(IMentionable::getAsMention));

            var data = data(message.getAuthor());
            String content = message.getContentRaw().toLowerCase(Locale.ROOT);

            //go through every ping individually
            for(var ping : mentioned){
                if(!ping.equals(data.lastPingId)){
                    data.lastPingId = ping;
                    data.uniquePings++;
                    if(data.uniquePings >= pingSpamLimit){
                        Log.info("Autobanning @ for spamming @ pings in a row.", message.getAuthor().getName() + "#" + message.getAuthor().getId(), data.uniquePings);
                        alertsChannel.sendMessage(message.getAuthor().getAsMention() + " **has been auto-banned for pinging " + pingSpamLimit + " unique members in a row!**").queue();
                        message.getGuild().ban(message.getAuthor(), 1, "Banned for spamming member pings. If you believe this was in error, file an issue on the CoreBot Github (https://github.com/Anuken/CoreBot/issues) or contact a moderator.").queue();
                    }
                }
            }

            if(mentioned.isEmpty()){
                data.uniquePings = 0;
            }

            //check for consecutive links
            if(!edit && linkPattern.matcher(content).find()){

                if(content.equals(data.lastLinkMessage) && !message.getChannel().getId().equals(data.lastLinkChannelId)){
                    Log.warn("User @ just spammed a link in @ (message: @): '@'", message.getAuthor().getName(), message.getChannel().getName(), message.getId(), content);

                    //only start deleting after 2 posts
                    if(data.linkCrossposts >= 1){
                        alertsChannel.sendMessage(
                            message.getAuthor().getAsMention() +
                            " **is spamming a link** in " + message.getTextChannel().getAsMention() +
                            ":\n\n" + message.getContentRaw()
                        ).queue();

                        message.delete().queue();
                        message.getAuthor().openPrivateChannel().complete().sendMessage("You have posted a link several times. Do not send any similar messages, or **you will be auto-banned.**").queue();
                    }

                    //4 posts = ban
                    if(data.linkCrossposts ++ >= 3){
                        Log.warn("User @ (@) has been auto-banned after spamming link messages.", message.getAuthor().getName(), message.getAuthor().getAsMention());

                        alertsChannel.sendMessage(message.getAuthor().getAsMention() + " **has been auto-banned for spam-posting links!**").queue();
                        message.getGuild().ban(message.getAuthor(), 1, "[Auto-Ban] Spam-posting links. If you are not a bot or spammer, please report this at https://github.com/Anuken/CoreBot/issues immediately!").queue();
                    }
                }

                data.lastLinkMessage = content;
                data.lastLinkChannelId = message.getChannel().getId();
            }else{
                data.linkCrossposts = 0;
                data.lastLinkMessage = null;
                data.lastLinkChannelId = null;
            }

            if(invitePattern.matcher(content).find()){
                Log.warn("User @ just sent a discord invite in @.", message.getAuthor().getName(), message.getChannel().getName());
                message.delete().queue();
                message.getAuthor().openPrivateChannel().complete().sendMessage("Do not send invite links in the Mindustry Discord server! Read the rules.").queue();
                return true;
            }else if(containsScamLink(message)){
                Log.warn("User @ just sent a potential scam message in @: '@'", message.getAuthor().getName(), message.getChannel().getName(), message.getContentRaw());

                int count = data.scamMessages ++;

                alertsChannel.sendMessage(
                    message.getAuthor().getAsMention() +
                    " **has sent a potential scam message** in " + message.getTextChannel().getAsMention() +
                    ":\n\n" + message.getContentRaw()
                ).queue();

                message.delete().queue();
                message.getAuthor().openPrivateChannel().complete().sendMessage("Your message has been flagged as a potential scam. Do not send any similar messages, or **you will be auto-banned.**").queue();

                if(count >= scamAutobanLimit - 1){
                    Log.warn("User @ (@) has been auto-banned after @ scam messages.", message.getAuthor().getName(), message.getAuthor().getAsMention(), count + 1);

                    alertsChannel.sendMessage(message.getAuthor().getAsMention() + " **has been auto-banned for posting " + scamAutobanLimit + " scam messages in a row!**").queue();
                    message.getGuild().ban(message.getAuthor(), 0, "[Auto-Ban] Posting several potential scam messages in a row. If you are not a bot or spammer, please report this at https://github.com/Anuken/CoreBot/issues immediately!").queue();
                }

                return true;
            }else{
                //non-consecutive scam messages don't count
                data.scamMessages = 0;
            }

        }
        return false;
    }

    boolean handleResponse(Message msg, CommandResponse response, boolean logUnknown){
        if(response.type == ResponseType.unknownCommand){
            if(logUnknown){
                errDelete(msg, "Error", "Unknown command. Type !help for a list of commands.");
            }
            return false;
        }else if(response.type == ResponseType.manyArguments || response.type == ResponseType.fewArguments){
            if(response.command.params.length == 0){
                errDelete(msg, "Invalid arguments.", "Usage: @@", prefix, response.command.text);
            }else{
                errDelete(msg, "Invalid arguments.", "Usage: @@ *@*", prefix, response.command.text, response.command.paramText);
            }
            return false;
        }
        return true;
    }

    boolean containsScamLink(Message message){
        String content = message.getContentRaw().toLowerCase(Locale.ROOT);

        //some discord-related keywords are never scams (at least, not from bots)
        if(notScamPattern.matcher(content).find()){
            return false;
        }

        // Regular check
        if(scamPattern.matcher(content.replace("\n", " ")).find()){
            return true;
        }

        // Extracts the urls of the message
        List<String> urls = urlPattern.matcher(content).results().map(MatchResult::group).toList();

        for(String url : urls){
            // Gets the domain and splits its different parts
            String[] rawDomain = url
                    .replace("https://", "")
                    .replace("http://", "")
                    .split("/")[0]
                    .split("\\.");

            // Gets rid of the subdomains
            rawDomain = Arrays.copyOfRange(rawDomain, Math.max(rawDomain.length - 2, 0), rawDomain.length);

            // Re-assemble
            String domain = String.join(".", rawDomain);

            // Matches slightly altered links
            if(!trustedDomains.contains(domain) && trustedDomains.stream().anyMatch(genuine -> Strings.levenshtein(genuine, domain) <= 2)){
                return true;
            }
        }

        return false;
    }

    UserData data(User user){
        return userData.get(user.getId(), UserData::new);
    }

    static class UserData{
        /** consecutive scam messages sent */
        int scamMessages;
        /** last message that contained any link */
        @Nullable String lastLinkMessage;
        /** channel ID of last link posted */
        @Nullable String lastLinkChannelId;
        /** link cross-postings in a row */
        int linkCrossposts;
        /** ID of last member pinged */
        String lastPingId;
        /** number of unique members pinged in a row */
        int uniquePings;
    }
}
