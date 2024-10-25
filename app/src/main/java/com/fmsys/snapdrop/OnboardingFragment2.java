package com.fmsys.snapdrop;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fmsys.snapdrop.databinding.FragmentOnboarding2Binding;
import com.fmsys.snapdrop.utils.Link;
import com.fmsys.snapdrop.utils.NetworkUtils;
import com.fmsys.snapdrop.utils.ViewUtils;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OnboardingFragment2 extends Fragment {

    public static class ServerItem {
        final String url;
        final String description;
        final String warning;

        ServerItem(final String url, final String description, final String warning) {
            this.url = url;
            this.description = description;
            this.warning = warning;
        }

    }

    public static class ServerItemViewHolder extends RecyclerView.ViewHolder {
        final TextView urlTextView;
        final TextView descriptionTextView;
        final TextView warningTextView;

        public ServerItemViewHolder(final View itemView) {
            super(itemView);
            urlTextView = itemView.findViewById(R.id.url);
            descriptionTextView = itemView.findViewById(R.id.description);
            warningTextView = itemView.findViewById(R.id.warning);
        }

        public void bind(final ServerItem item) {
            urlTextView.setText(item.url);
            descriptionTextView.setText(item.description);
            descriptionTextView.setVisibility(item.description == null ? View.GONE : View.VISIBLE);
            warningTextView.setText(item.warning);
            warningTextView.setVisibility(item.warning == null ? View.GONE : View.VISIBLE);
        }
    }

    public class ServerItemCardAdapter extends RecyclerView.Adapter<ServerItemViewHolder> {

        private final List<ServerItem> items;

        public ServerItemCardAdapter(final List<ServerItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ServerItemViewHolder onCreateViewHolder(final @NonNull ViewGroup parent, final int viewType) {
            final View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.servercard, parent, false);

            final ServerItemViewHolder holder = new ServerItemViewHolder(view);

            holder.itemView.setOnClickListener(v -> {
                tempUrl.setValue(holder.urlTextView.getText().toString());
            });

            holder.itemView.setOnLongClickListener(v -> {
                removeServer(holder.urlTextView.getText().toString());
                return true;
            });

            tempUrl.observe(requireActivity(), url -> {
                ((MaterialCardView) holder.itemView).setChecked(holder.urlTextView.getText().toString().equals(url));
            });

            return holder;
        }

        @Override
        public void onBindViewHolder(final @NonNull ServerItemViewHolder holder, final int position) {
            holder.bind(items.get(position));
            ((MaterialCardView) holder.itemView).setChecked(holder.urlTextView.getText().toString().equals(tempUrl.getValue()));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    final MutableLiveData<String> tempUrl = new MutableLiveData<>();

    FragmentOnboarding2Binding binding;
    SharedPreferences pref;

    public OnboardingFragment2() {
        super(R.layout.fragment_onboarding_2);
    }

    @Override
    public void onViewCreated(final @NonNull View view, final Bundle savedInstanceState) {

        final OnboardingViewModel viewModel = new ViewModelProvider(requireActivity()).get(OnboardingViewModel.class);

        binding = FragmentOnboarding2Binding.bind(view);
        pref = PreferenceManager.getDefaultSharedPreferences(requireContext());
        tempUrl.setValue(pref.getString(getString(R.string.pref_baseurl), "https://pairdrop.net"));

        reloadServerList();

        binding.add.setOnClickListener(v -> ViewUtils.showEditTextWithResetPossibility(this, "Custom URL", null, null, Link.bind("https://github.com/RobinLinus/snapdrop/blob/master/docs/faq.md#inofficial-instances", R.string.baseurl_unofficial_instances), url -> {
            if (url == null) {
                return;
            }

            if (url.startsWith("!!")) { // hidden feature to force a different url
                newServer(url.substring("!!".length()));
            } else if (url.startsWith("http")) {
                NetworkUtils.checkInstance(this, url, result -> {
                    if (result) {
                        newServer(url);
                    }
                });
            } else {

                // do some magic in case user forgot to specify the protocol

                String mightBeHttpsUrl = "https://" + url;
                NetworkUtils.checkInstance(this, mightBeHttpsUrl, resultHttps -> {
                    if (resultHttps) {
                        newServer(mightBeHttpsUrl);
                    } else {
                        String mightBeHttpUrl = "http://" + url;
                        NetworkUtils.checkInstance(this, mightBeHttpUrl, resultHttp -> {
                            if (resultHttp) {
                                newServer(mightBeHttpUrl);
                            }
                        });
                    }
                });
            }
        }));

        binding.continueButton.setOnClickListener(v -> {
            viewModel.url(tempUrl.getValue());
            if (viewModel.isOnlyServerSelection()) {
                requireActivity().finish();
            } else {
                viewModel.launchFragment(OnboardingFragment3.class);
            }
        });
        binding.continueButton.requestFocus();
    }

    private void reloadServerList() {
        final Set<String> serverUrls = pref.getStringSet(getString(R.string.pref_custom_servers), new HashSet<>());

        final List<ServerItem> servers = new ArrayList<>();
        servers.add(new ServerItem("https://pairdrop.net", getString(R.string.onboarding_server_pairdrop_summary), null));
        servers.add(new ServerItem("https://snapdrop.net", getString(R.string.onboarding_server_snapdrop_summary), null/*getString(R.string.onboarding_server_snapdrop_summary_server_warning)*/));

        for (String url : serverUrls) {
            servers.add(new ServerItem(url, null, null));
        }

        binding.listview.setAdapter(new ServerItemCardAdapter(servers));
    }

    private void newServer(final String url) {
        final Set<String> serverUrls = new HashSet<>(pref.getStringSet(getString(R.string.pref_custom_servers), new HashSet<>()));
        serverUrls.add(url);
        pref.edit().putStringSet(getString(R.string.pref_custom_servers), serverUrls).apply();
        tempUrl.setValue(url);
        reloadServerList();
    }

    private void removeServer(final String url) {
        final Set<String> serverUrls = new HashSet<>(pref.getStringSet(getString(R.string.pref_custom_servers), new HashSet<>()));
        serverUrls.remove(url);
        pref.edit().putStringSet(getString(R.string.pref_custom_servers), serverUrls).apply();
        reloadServerList();
    }

}
