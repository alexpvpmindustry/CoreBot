package corebot;

import arc.files.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.Nullable;
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
import net.dv8tion.jda.api.events.message.react.*;
import net.dv8tion.jda.api.hooks.*;
import net.dv8tion.jda.api.requests.*;
import net.dv8tion.jda.api.utils.*;
import net.dv8tion.jda.api.utils.cache.*;
import org.jetbrains.annotations.*;

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
    private static final int scamAutobanLimit = 3, pingSpamLimit = 20, minModStars = 10, naughtyTimeoutMins = 20;
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
        "скин.*partner",
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
        "бесплат.*нитро.*http",
        "нитро.*бесплат.*http",
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

    public Messages(){
        String token = System.getenv("CORE_BOT_TOKEN");

        register();

        try{
            jda = JDABuilder.createDefault(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_EMOJIS, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MESSAGE_REACTIONS)
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
        guild = jda.getGuildById(391020510269669376L);
        // alex removed pluginChannel = channel(617833229973717032L);
        // alex removed crashReportChannel = channel(467033526018113546L);
        // alex removed announcementsChannel = channel(391020997098340352L);
        // alex removed artChannel = channel(754011833928515664L);
        mapsChannel = channel(416719902641225732L);
        // alex removed moderationChannel = channel(488049830275579906L);
        schematicsChannel = channel(640604827344306207L);
        baseSchematicsChannel = channel(718536034127839252L);
        // alex removed logChannel = channel(568416809964011531L);
        // alex removed joinChannel = channel(832688792338038844L);
        // alex removed streamsChannel = channel(833420066238103604L);
        // alex removed videosChannel = channel(833826797048692747L);
        // alex removed testingChannel = channel(432984286099144706L);
        // alex removed alertsChannel = channel(864139464401223730L);
        curatedSchematicsChannel = channel(878022862915653723L);

        schematicChannels.add(schematicsChannel.getIdLong(), baseSchematicsChannel.getIdLong(), curatedSchematicsChannel.getIdLong());
    }

    void printCommands(CommandHandler handler, StringBuilder builder){
        for(Command command : handler.getCommandList()){
            builder.append(prefix);
            builder.append("**");
            builder.append(command.text);
            builder.append("**");
            if(command.params.length > 0){
                builder.append(" *");
                builder.append(command.paramText);
                builder.append("*");
            }
            builder.append(" - ");
            builder.append(command.description);
            builder.append("\n");
        }
    }

    void register(){
        handler.<Message>register("help", "Displays all bot commands.", (args, msg) -> {
            StringBuilder builder = new StringBuilder();
            printCommands(handler, builder);
            info(msg.getChannel(), "Commands", builder.toString());
        });

        handler.<Message>register("postmap", "Post a .msav file to the #maps channel.", (args, msg) -> {

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

    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event){
        try{

            var msg = event.getMessage();

            if(msg.getAuthor().isBot() || msg.getChannel().getType() != ChannelType.TEXT) return;

            if(msg.getMentionedUsers().contains(jda.getSelfUser())){
                msg.addReaction(aaaaa).queue();
            }

            EmbedBuilder log = new EmbedBuilder()
            .setAuthor(msg.getAuthor().getName(), msg.getAuthor().getEffectiveAvatarUrl(), msg.getAuthor().getEffectiveAvatarUrl())
            .setDescription(msg.getContentRaw().length() >= 2040 ? msg.getContentRaw().substring(0, 2040) + "..." : msg.getContentRaw())
            .addField("Author", msg.getAuthor().getAsMention(), false)
            .addField("Channel", msg.getTextChannel().getAsMention(), false)
            .setColor(normalColor);

            for(var attach : msg.getAttachments()){
                log.addField("File: " + attach.getFileName(), attach.getUrl(), false);
            }

            if(msg.getReferencedMessage() != null){
                log.addField("Replying to", msg.getReferencedMessage().getAuthor().getAsMention() + " [Jump](" + msg.getReferencedMessage().getJumpUrl() + ")", false);
            }

            if(msg.getMentionedUsers().stream().anyMatch(u -> u.getIdLong() == 123539225919488000L)){
                log.addField("Note", "thisisamention", false);
            }

//        // alex removed if(msg.getChannel().getIdLong() != testingChannel.getIdLong()){
//            logChannel.sendMessageEmbeds(log.build()).queue();
//        }

        //delete stray invites

        //delete non-art

            String text = msg.getContentRaw();

            //schematic preview
            if((msg.getContentRaw().startsWith(ContentHandler.schemHeader) && msg.getAttachments().isEmpty()) ||
            (msg.getAttachments().size() == 1 && msg.getAttachments().get(0).getFileExtension() != null && msg.getAttachments().get(0).getFileExtension().equals(Vars.schematicExtension))){
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

                    msg.getChannel().sendFile(schemFile).addFile(previewFile).setEmbeds(builder.build()).queue();
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
            }else if(schematicChannels.contains(msg.getChannel().getIdLong()) && !isAdmin(msg.getAuthor())){
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
        }catch(Exception e){
            Log.err(e);
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
        return member != null && member.getRoles().stream().anyMatch(role -> role.getName().equals("Developer") || role.getName().equals("Moderator") || role.getName().equals("\uD83D\uDD28 \uD83D\uDD75️\u200D♂️"));
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
        /** all members pinged in consecutive messages */
        ObjectSet<String> idsPinged = new ObjectSet<>();
    }
}
